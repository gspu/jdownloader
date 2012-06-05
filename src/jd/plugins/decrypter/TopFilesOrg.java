//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "topfiles.org" }, urls = { "http://(www\\.)?topfiles\\.org/(download/[A-Za-z0-9]+/[^<>\"/]*?\\.html|list/[A-Za-z0-9]+)" }, flags = { 0 })
public class TopFilesOrg extends PluginForDecrypt {

    public TopFilesOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getHeaders().put("Referer", "http://topfiles.org/");
        br.getPage(parameter);
        // Check if its a single link (folder- and singlelinks look the same)
        String singleLink = br.getRedirectLocation();
        if (singleLink != null && !this.canHandle(singleLink)) {
            decryptedLinks.add(createDownloadlink(singleLink));
            return decryptedLinks;
        }
        if (singleLink != null) {
            br.getPage(singleLink);
            singleLink = br.getRedirectLocation();
            if (singleLink != null && !this.canHandle(singleLink)) {
                if (singleLink.contains("goo.gl")) {
                    br.getPage(singleLink);
                    singleLink = br.getRedirectLocation();
                }
                decryptedLinks.add(createDownloadlink(singleLink));
                return decryptedLinks;
            }
        }
        if (singleLink != null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        final String[] links = br.getRegex("<td class=\"middle\"><a href=\"(http://topfiles\\.org/download/[^<>\"]*?)\"").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String crLink : links) {
            if (parameter.equalsIgnoreCase(crLink)) continue;
            decryptedLinks.add(createDownloadlink(crLink));
        }
        return decryptedLinks;
    }

}
