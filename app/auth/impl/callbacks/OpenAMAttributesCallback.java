package auth.impl.callbacks;

import java.util.ArrayList;
import java.util.HashMap;

import auth.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.libs.F.Function;
import play.libs.ws.WS;
import play.libs.ws.WSResponse;
import play.mvc.Http;
import play.mvc.Http.Cookie;

public class OpenAMAttributesCallback extends HeadlessCallback {

    private static Logger logger = LoggerFactory.getLogger(OpenAMAttributesCallback.class);
    private ArrayList<String> lst;
    private String openAMUrl;
    private HashMap<String,String> attrs = new HashMap<String,String>();

    /**
     * @param openAmUrl
     * @param lst
     */
    public OpenAMAttributesCallback(String openAmUrl, ArrayList<String> lst) {
        this.lst = lst;
        this.openAMUrl = openAmUrl;
    }

    @Override
    public void process() {
        if (openAMUrl == null) {
            logger.trace("OpenAM URL not configured!");
        }
        Http.Request req = getOriginalRequest();
        Cookie c = req.cookies().get("iPlanetDirectoryPro");
        if (c == null) {
            logger.trace("Not Authenticated!!!");
            return;
        }
        String token = c.value();
        //logger.debug(openAMUrl + "/identity/attributes");
        String result = WS.url(openAMUrl + "/identity/attributes")
                .setQueryParameter("subjectid", java.net.URLEncoder.encode(token))
                .get().map(new Function<WSResponse, String>() {
                    @Override
                    public String apply(WSResponse response) {
                        return response.getBody();
                    }
                }).get(Configuration.MAX_TIMEOUT);
        //logger.debug(result);
        String nm = "userdetails.attribute.name=";
        String val = "userdetails.attribute.value=";
        int nmlength = nm.length();
        int vallength = val.length();
        for (String attr : lst) {
            int idx = result.indexOf(nm + attr);
            if (idx < 0) continue;
            int startidx = idx + nmlength + attr.length() + vallength + 1;
            int endidx = result.indexOf(nm, startidx) - 1; // subtract 1 for CR
            String value = result.substring(startidx, endidx);
            attrs.put(attr, value);
            //logger.debug("found - " + attr + "='" + value + "'");
        }
        logger.debug("OpenAM attributes: " + attrs);
    }

    public String getValue(String attr) {
        return attrs.get(attr);
    }

}
