# Load testing

**IMPORTANT:** Inform the cloud platform team before running load tests against any environment.

1. Install JMeter using brew: `brew install jmeter`
2. Open the JMeter GUI: `jmeter`
3. Use the plugin manager (Options -> Plugins Manager or the icon on the top right) to install the [Custom JMeter Functions](https://jmeter-plugins.org/wiki/Functions/) plugin
4. Close JMeter and run the following command to open the test plan and specify required properties:

```
JVM_ARGS="-Xms1024m -Xmx1024m" jmeter -t load-test.jmx -Jbase_protocol='http' -Jbase_server_name='localhost' -Jbase_port='8080' -Jauth_server_name='<auth_server_name>' -Jclient_id='<client_id>' -Jclient_secret='<client_secret>'
```

## Running the load tests from the command line

```
rm -rf load-test-* \
&& JVM_ARGS="-Xms1024m -Xmx1024m" jmeter -n -t load-test.jmx -l load-test-results.jtl -e -o load-test-results -Jbase_protocol='http' -Jbase_server_name='localhost' -Jbase_port='8080' -Jauth_server_name='<auth_server_name>' -Jclient_id='<client_id>' -Jclient_secret='<client_secret>' \
&& open load-test-results/index.html
```
