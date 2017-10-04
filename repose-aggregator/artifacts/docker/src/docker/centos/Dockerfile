# Use CentOS as a base, to enable security patching for
# CentOS users.
# The version should be whatever version conforms with
# the managed CentOS image.
FROM centos:7

MAINTAINER The Repose Team <reposecore@rackspace.com>

# This build-arg is used to pass the Repose version number which will be set up in this image.
ARG REPOSE_VERSION

# Install Repose (and Java) from the Debian package repository.
RUN yum update -q -y && yum install -q -y wget java-1.8.0-openjdk-headless
RUN wget --quiet -O /etc/yum.repos.d/openrepose.repo http://repo.openrepose.org/el/openrepose.repo
RUN yum update -q -y && yum install -q -y repose-valve-$REPOSE_VERSION repose-filter-bundle-$REPOSE_VERSION repose-extensions-filter-bundle-$REPOSE_VERSION repose-experimental-filter-bundle-$REPOSE_VERSION

ENV APP_ROOT=/etc/repose
ENV APP_VARS=/var/repose

# Turn off local logging
RUN sed -i 's,<\(Appender.*RollingFile.*/\)>,<!--\1-->,' ${APP_ROOT}/log4j2.xml
RUN sed -i 's,<\(Appender.*PhoneHomeMessages.*/\)>,<!--\1-->,' ${APP_ROOT}/log4j2.xml

# Arbitrary User ID support
RUN chgrp -R 0 ${APP_ROOT} ${APP_VARS} && \
    chmod -R g=u ${APP_ROOT} ${APP_VARS}

# Expose APP_ROOT as a volume for mounting.
WORKDIR ${APP_ROOT}
VOLUME ${APP_ROOT}

# Switch user to repose
USER repose

# Expose the default Repose service port for host port forwarding.
# If the port in the user's system model differs from this port, the user will have to map it manually using the
# "-p" flag with the Docker run command.
EXPOSE 8080

# This environment variable is used to set command-line options.
# The user can manually set these options using the "-e" flag with the Docker run command.
ENV JAVA_OPTS=

# Start Repose.
CMD java $JAVA_OPTS -jar /usr/share/repose/repose-valve.jar -c /etc/repose