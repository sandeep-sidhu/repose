echo "-------------------------------------------------------------------------------------------------------------------"
echo "Starting httpbin"
echo "-------------------------------------------------------------------------------------------------------------------"

gunicorn httpbin:app &
