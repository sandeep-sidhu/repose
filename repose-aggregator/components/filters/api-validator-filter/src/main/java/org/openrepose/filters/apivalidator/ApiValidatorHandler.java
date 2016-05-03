/*
 * _=_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=
 * Repose
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Copyright (C) 2010 - 2015 Rackspace US, Inc.
 * _-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_-_=_
 */
package org.openrepose.filters.apivalidator;

import com.rackspace.com.papi.components.checker.Validator;
import com.rackspace.com.papi.components.checker.step.results.ErrorResult;
import com.rackspace.com.papi.components.checker.step.results.Result;
import org.openrepose.commons.utils.http.OpenStackServiceHeader;
import org.openrepose.commons.utils.servlet.http.HttpServletRequestWrapper;
import org.openrepose.core.filters.ApiValidator;
import org.openrepose.core.services.reporting.metrics.MetricsService;
import org.openrepose.core.services.reporting.metrics.MeterByCategorySum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class ApiValidatorHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ApiValidatorHandler.class);
    private final List<ValidatorInfo> validators;
    private final ValidatorInfo defaultValidator;
    private Set<String> matchedRoles;
    private boolean multiRoleMatch = false;
    private boolean delegatingMode;
    private MeterByCategorySum mbcsInvalidRequests;

    public ApiValidatorHandler(
            ValidatorInfo defaultValidator,
            List<ValidatorInfo> validators,
            boolean multiRoleMatch,
            boolean delegatingMode,
            MetricsService metricsService) {
        this.validators = new ArrayList<>(validators.size());
        this.matchedRoles = new HashSet<>();
        this.validators.addAll(validators);
        this.multiRoleMatch = multiRoleMatch;
        this.defaultValidator = defaultValidator;
        this.delegatingMode = delegatingMode;

        // TODO replace "api-validator" with filter-id or name-number in sys-model
        if (metricsService != null) {
            mbcsInvalidRequests = metricsService.newMeterByCategorySum(ApiValidator.class,
                    "api-validator", "InvalidRequest", TimeUnit.SECONDS);
        }
    }

    private boolean appendDefaultValidator(List<ValidatorInfo> validatorList) {
        if (defaultValidator != null) {
            if (!multiRoleMatch) {
                validatorList.add(defaultValidator);
            } else if (!validatorList.contains(defaultValidator)) {
                validatorList.add(0, defaultValidator);
            }

            return true;
        }

        return false;
    }

    protected List<ValidatorInfo> getValidatorsForRoles(List<String> listRoles) {
        Set<ValidatorInfo> validatorSet = new LinkedHashSet<>();
        Set<String> roles = new HashSet<>(listRoles);

        for (ValidatorInfo validator : validators) {
            for (String validatorRoles : validator.getRoles()) {
                if (roles.contains(validatorRoles)) {
                    validatorSet.add(validator);
                    matchedRoles.add(validatorRoles);
                }
            }
        }

        List<ValidatorInfo> validatorList = new ArrayList<>(validatorSet);
        if (appendDefaultValidator(validatorList)) {
            matchedRoles.addAll(roles);
        }

        return !multiRoleMatch && !validatorList.isEmpty() ? validatorList.subList(0, 1) : validatorList;
    }

    private ErrorResult getErrorResult(Result lastResult) {
        if (lastResult instanceof ErrorResult) {
            return (ErrorResult) lastResult;
        }

        return null;
    }

    private void sendMultiMatchErrorResponse(Result result, HttpServletResponse response) {
        try {
            ErrorResult error = getErrorResult(result);
            if (error != null && !delegatingMode) {
                response.setStatus(error.code());
                response.sendError(error.code(), error.message());
            }
        } catch (Throwable t) {
            // API Checker throws exceptions that extend Throwable
            LOG.error("Some error", t);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }

    public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        HttpServletRequestWrapper wrappedRequest = new HttpServletRequestWrapper(request);
        List<String> roles = wrappedRequest.getPreferredSplittableHeaders(OpenStackServiceHeader.ROLES.toString());
        if (roles.isEmpty()) {
            roles = Collections.singletonList("");
        }
        Result lastValidatorResult = null;
        boolean isValid = false;

        try {
            matchedRoles.clear();
            List<ValidatorInfo> matchedValidators = getValidatorsForRoles(roles);
            if (!matchedValidators.isEmpty()) {
                for (ValidatorInfo validatorInfo : matchedValidators) {

                    Validator validator = validatorInfo.getValidator();
                    if (validator == null) {
                        LOG.warn("Validator not available for request: {}", validatorInfo.getUri());
                        response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
                    } else {
                        /** NOTE: This note should really be on DelegationHandler in api-checker, but that code is
                         * not part of this project. Anyway, api-checker is definitely closing the request input
                         * stream in certain cases. The problem is, if we're delegating, api-checker takes the
                         * request with a closed input stream and uses it to call doFilter (in the
                         * DelegationHandler). Once that is done, no downstream component can read the input stream,
                         * period.
                         *
                         * As an aside, since api-checker actually returns super.getInputStream in some cases,
                         * the input stream of the request passed to a validator may be closed. In that case, unless
                         * some component upstream passed an InputStream that can be read after having close() called on
                         * it, the request InputStream will be closed and unreadable to all upstream components as well.
                         *
                         * The stream is closed by the XSL class in api-checker. Specifically, the checkStep(...) method
                         * calls transformer.transform(...) which closes the stream.
                         *
                         * The best solution likely lies in api-checker. The InputStream should not be closed when
                         * delegating.
                         * Alternatively, the ApiValidatorHandler could be responsible for calling doFilter so that
                         * it can wrap the request appropriately. Of course, since api-checker enriches the request,
                         * if the ApiValidatorHandler took over calling doFilter, it would need to do so by passing
                         * a modifier filter chain where doFilter calls back into the handler. That way, the handler
                         * would have all of the enriched data, and if the InputStream is closed, the handler could
                         * revert to using the InputStream before it was read/closed by api-checker.
                          */
                        lastValidatorResult = validator.validate(wrappedRequest, response, chain);
                        isValid = lastValidatorResult.valid();
                        if (isValid) {
                            break;
                        }
                    }
                }

                if (!isValid) {
                    if (mbcsInvalidRequests != null) {
                        for (String s : matchedRoles) {
                            mbcsInvalidRequests.mark(s);
                        }
                    }
                    if (multiRoleMatch) {
                        sendMultiMatchErrorResponse(lastValidatorResult, response);
                    }
                }
            } else {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
            }
        } catch (Throwable t) {
            // API Checker throws exceptions that extend Throwable
            LOG.error("Error processing validation", t);
            response.setStatus(HttpServletResponse.SC_BAD_GATEWAY);
        }
    }
}
