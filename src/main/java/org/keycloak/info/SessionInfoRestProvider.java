package org.keycloak.info;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.entities.ClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.services.resource.RealmResourceProvider;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SessionInfoRestProvider implements RealmResourceProvider {

    protected static final Logger log = Logger.getLogger(SessionInfoRestProvider.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private Cache<String, SessionEntity> ispnCache;
    private String initialInfo;

    public SessionInfoRestProvider(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
    }

    @Override
    public Object getResource() {
        return this;
    }

    @GET
    @Path("/get-session")
    @Produces(MediaType.TEXT_HTML)
    public String getSession(@QueryParam("id") final String sessionId, @QueryParam("offline") final String offline, @QueryParam("local") String local) {
        Cache<String, SessionEntity> cache = init(offline, local);
        String message;
        if (sessionId == null) {
            message = "Parameter 'id' not provided!";
        } else {
            UserSessionEntity entity = (UserSessionEntity) cache.get(sessionId);
            message = entity==null ? "Not found session with Id: " + sessionId + "<br>"
                    : "Found session. " + toString(entity);
        }

        return initialInfo + message;
    }

    @GET
    @Path("/list-sessions")
    @Produces(MediaType.TEXT_HTML)
    public String listSessions(@QueryParam("offline") final String offline, @QueryParam("local") String local) {
        Cache<String, SessionEntity> cache = init(offline, local);

        StringBuilder message = new StringBuilder(initialInfo + "User sessions: <br>");
        for (String id : cache.keySet()) {
            SessionEntity entity = cache.get(id);
            if (!(entity instanceof UserSessionEntity)) {
                continue;
            }
            UserSessionEntity userSession = (UserSessionEntity) cache.get(id);
            message.append("UserSession: <br>" + toString(userSession));
        }

        return message.toString();
    }


    @GET
    @Path("/size")
    @Produces(MediaType.TEXT_HTML)
    public String size(@QueryParam("offline") final String offline, @QueryParam("local") String local) {
        Cache<String, SessionEntity> cache = init(offline, local);

        return initialInfo + "Cache size: " + cache.size();
    }


    private Cache<String, SessionEntity> init(String offline, String local) {
        // true by default
        boolean isOffline = (offline == null || offline.isEmpty() || offline.equalsIgnoreCase("true"));

        // false by default
        boolean isLocal = (local != null && local.equalsIgnoreCase(local));

        initialInfo = "offline=" + isOffline + ", local=" + isLocal + "<br><br>";

        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        this.ispnCache = isOffline ? provider.getCache(InfinispanConnectionProvider.OFFLINE_SESSION_CACHE_NAME) : provider.getCache(InfinispanConnectionProvider.SESSION_CACHE_NAME);

        if (isLocal) {
            return this.ispnCache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL);
        } else {
            return this.ispnCache;
        }
    }


    private String toString(UserSessionEntity userSession) {
        String message = "ID: " + userSession.getId() + ", Started: " + Time.toDate(userSession.getStarted()) + ", LastSessionRefresh: " + Time.toDate(userSession.getLastSessionRefresh()) +
                "<br>"
                + "Client sessions: <br>";

        if (userSession.getClientSessions() != null) {
            for (String clientSessionId : userSession.getClientSessions()) {
                ClientSessionEntity cle = (ClientSessionEntity) ispnCache.get(clientSessionId);
                if (cle == null) {
                    message = message + "ERROR: Not found clientSession " + clientSessionId + "<br>";
                } else {
                    message = message + "ClientSessionId: " + cle.getId() + ", Timestamp: " + Time.toDate(cle.getTimestamp()) + "<br>";
                }
            }
        }

        return message + "<br>";
    }


    @Override
    public void close() {
    }

}
