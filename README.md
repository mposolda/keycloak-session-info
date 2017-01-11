How to use this
===============
1) Build with `mvn clean install`

2) Assume you have 2 Keycloak servers, which will be in cluster. Install this module to both nodes by run this command for each node:

````
$KEYCLOAK_HOME/bin/jboss-cli.sh --command="module add --name=org.keycloak.examples.keycloak-session-info --resources=target/keycloak-session-info.jar --dependencies=org.keycloak.keycloak-core,org.keycloak.keycloak-common,org.keycloak.keycloak-server-spi,org.keycloak.keycloak-server-spi-private,javax.ws.rs.api,org.keycloak.keycloak-model-infinispan,org.infinispan,org.jboss.logging"
````


3) Add this to `$KEYCLOAK_HOME/standalone/configuration/standalone-ha.xml` on both nodes:


````
<provider>module:org.keycloak.examples.keycloak-session-info</provider>
````

4) Run both nodes and add some offline sessions. You can use REST endpoints like this:


This is to see the size of the offlineSessions cache:

````
http://node1:8080/auth/realms/master/session-info/size
````

This is to list the sessions on the offlineSessions cache (NOTE: This will fill the sessions to the l1 cache on the node where it is executed. 
So the sessions will be there for at least 10 minutes until l1 cache is expired and they will be available locally on that node (See below for the `local` parameter):

````
http://node1:8080/auth/realms/master/session-info/list-sessions
````

This is to lookup particular session:

````
http://node1:8080/auth/realms/master/session-info/get-session?id=870d4726-9d9e-438c-b3e5-69d0a7b52098
````


The parameter `local=false` is to use the particular command with the infinispan cache in local mode (Flag CACHE_MODE_LOCAL used on that cache):
````
http://node1:8080/auth/realms/master/session-info/size?local=false
````

Finally the parameter `offline=false` is to lookup the `sessions` cache instead of `offlineSessions` cache:
````
http://node1:8080/auth/realms/master/session-info/size?offline=false
````


EXAMPLE OF USAGE:
When you start both servers and offline sessions are preloaded from DB. Some are "owned" by the node1 and some by the node2. When you send this request:

````
http://node1:8080/auth/realms/master/session-info/size
````

You will see same value on both nodes. But when you run it just locally, you will see just the size of the locally saved session for particular node. 
````
http://node1:8080/auth/realms/master/session-info/size?local=true
http://node2:8080/auth/realms/master/session-info/size?local=true
````

The local sum on both nodes should be the overall size returned by the request without "local" parameter.
 
Once you change owners to 2 in standalone-ha.xml, then all the records should be duplicated on both nodes. 

 
````
<distributed-cache name="offlineSessions" mode="SYNC" owners="2"/>
````

So even with `local=true` you will see the same size on both nodes like 
without `local=true`.
 # keycloak-session-info
