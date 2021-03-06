//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperCrawlerPluginRecaptchaV2;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

/**
 * @author typek_pb
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "avxhm.se" }, urls = { "https?://(www\\.)?(avaxhome\\.(?:ws|bz|cc|in)|avaxho\\.me|avaxhm\\.com|avxhm\\.is|avxhome\\.(?:se|in)|avxhm\\.se|avaxhome\\.unblocker\\.xyz)/(ebooks|music|software|video|magazines|newspapers|games|graphics|misc|hraphile|comics|go)/.+|https?://(www\\.)?(avaxhome\\.pro)/[A-Za-z0-9\\-_]+\\.html" })
public class AvxHmeW extends PluginForDecrypt {
    @SuppressWarnings("deprecation")
    public AvxHmeW(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String notThis = "(?:https?:)?(?://(?!(www\\.imdb\\.com|avaxhome\\.(?:ws|bz|cc|in)|avaxho\\.me|avaxhm\\.com|avxhm\\.is|avxhome\\.(?:se|in)|avaxhome\\.pro|avxsearch\\.(?:se|pro))))[\\S&]+";

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink cryptedLink, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // for when you're testing
        br = new Browser();
        br.setAllowedResponseCodes(new int[] { 401 });
        // two different sites, do not rename, avaxhome.pro doesn't belong to the following template.
        final String parameter = cryptedLink.toString().replaceAll("(avaxhome\\.(?:ws|bz|cc|in)|avaxho\\.me|avaxhm\\.com|avxhm\\.is|avxhome\\.(?:se|in)|avxhm\\.se|avaxhome\\.unblocker\\.xyz)", "avxhm.se");
        if (parameter.matches(".*/go/\\d+/.*")) {
            /* 2021-01-20: Login whenever possible -> No captchas required then */
            final Account acc = AccountController.getInstance().getValidAccount(this.getHost());
            if (acc != null) {
                final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
                ((jd.plugins.hoster.AvxHmeW) hostPlugin).login(acc, false);
            }
            br.setFollowRedirects(false);
            br.getPage(parameter);
            followInternalRedirects();
            String link = br.getRedirectLocation();
            if (link == null) {
                final Form captchaForm = br.getForm(0);
                if (captchaForm.hasInputFieldByName("g-recaptcha-response")) {
                    final String siteURL = br.getURL("/").toString();
                    final String recaptchaV2Response = new CaptchaHelperCrawlerPluginRecaptchaV2(this, br) {
                        protected String getSiteUrl() {
                            // special handling
                            // being logged in can result in auto redirect/no captcha
                            return siteURL;
                        };
                    }.getToken();
                    captchaForm.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
                }
                br.submitForm(captchaForm);
                followInternalRedirects();
                link = br.getRedirectLocation();
            }
            if (link != null && !link.matches(this.getSupportedLinks().pattern())) {
                decryptedLinks.add(createDownloadlink(link));
            } else {
                logger.warning("Failed to find any result");
            }
        } else {
            br.setFollowRedirects(true);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return decryptedLinks;
            }
            final HashSet<String> dupe = new HashSet<String>();
            if (!parameter.contains("avaxhome.pro/")) {
                // 1.st try: <a href="LINK" target="_blank" rel="nofollow"> but ignore
                // images/self site refs + imdb refs
                String[] links = br.getRegex("<a[^>]*?href=\"(/go/\\d+/[^\"]+)").getColumn(0);
                if (links == null || links.length == 0) {
                    links = br.getRegex("<a href=\"(" + notThis + ")\"(?:\\s+[^>]*target=\"_blank\" rel=\"nofollow[^>]*|>Download from)").getColumn(0);
                }
                if (links != null && links.length != 0) {
                    for (String link : links) {
                        if (!dupe.add(link)) {
                            continue;
                        }
                        if (!link.matches(this.getSupportedLinks().pattern()) || link.startsWith("/go/")) {
                            decryptedLinks.add(createDownloadlink(br.getURL(link).toString()));
                        }
                    }
                }
                // try also LINK</br>, but ignore self site refs + imdb refs
                links = null;
                links = br.getRegex("(" + notThis + ")<br\\s*/\\s*>").getColumn(0);
                if (links != null && links.length != 0) {
                    for (String link : links) {
                        // strip html tags
                        link = link.replaceAll("<[^>]+>", "");
                        if (!dupe.add(link)) {
                            continue;
                        }
                        if (!link.matches(this.getSupportedLinks().pattern())) {
                            decryptedLinks.add(createDownloadlink(link));
                        }
                    }
                }
                final String[] covers = br.getRegex("\"((?:https?:)?//(pi?xhst|pixhost)\\.(com|co|icu)[^<>\"]*?)\"").getColumn(0);
                if (covers != null && covers.length != 0) {
                    for (String coverlink : covers) {
                        coverlink = Request.getLocation(coverlink, br.getRequest());
                        if (!dupe.add(coverlink)) {
                            continue;
                        }
                        decryptedLinks.add(createDownloadlink(coverlink));
                    }
                }
                String fpName = br.getRegex("<title>(.*?)\\s*[\\|/]\\s*AvaxHome.*?</title>").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("<h1>(.*?)</h1>").getMatch(0);
                }
                if (fpName != null) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(Encoding.htmlOnlyDecode(fpName.trim()));
                    fp.addLinks(decryptedLinks);
                }
            } else {
                br.setFollowRedirects(false);
                String[] links = br.getRegex("<h3>Download Link: <a href=\"https?://(www\\.)?avaxhome\\.pro/[a-z0-9\\-_]+/(\\d+)\"").getColumn(1);
                if (links != null && links.length != 0) {
                    for (final String id : links) {
                        br.getPage("http://www.avaxhome.pro/wp-content/plugins/download-monitor/download.php?id=" + id);
                        String redirect = br.getRedirectLocation();
                        if (redirect == null) {
                            logger.warning("Decrypter broken for link: " + parameter);
                            return null;
                        }
                        if (!redirect.matches(this.getSupportedLinks().pattern())) {
                            decryptedLinks.add(createDownloadlink(redirect));
                        }
                    }
                }
            }
        }
        return decryptedLinks;
    }

    private void followInternalRedirects() throws IOException {
        while (true) {
            final String link = br.getRedirectLocation();
            if (link != null && link.matches(this.getSupportedLinks().pattern()) && link.matches("^https?://.+")) {
                br.followRedirect();
            } else {
                break;
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}