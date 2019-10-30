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
package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.components.ImgShotCore;

import jd.PluginWrapper;
import jd.plugins.HostPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class AcidimgCc extends ImgShotCore {
    public AcidimgCc(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * mods: See overridden functions<br />
     * limit-info:<br />
     * captchatype-info: null<br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "acidimg.cc" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return ImgShotCore.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    protected boolean enforceOfficialDownloadURL() {
        /* 2019-10-30: Confirmed working */
        return true;
    }

    @Override
    protected String regexFilename() {
        /* 2019-10-30: special */
        String filename = super.regexFilename();
        if (filename == null) {
            filename = br.getRegex("<title>ACiDiMG /([^<>\"]+)</title>").getMatch(0);
        }
        return filename;
    }
}