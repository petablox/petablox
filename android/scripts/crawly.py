# -*- coding: utf-8 -*-
#
#  crawly.py : Crawl multiple android marketplaces, retrieve APK files.
#
#  Originally written by: Lazaro Clapp

# ============================================================================
# Imports
# ============================================================================

import os
import sgmllib
import subprocess
import sys
import urllib
import urlparse

# ============================================================================
# Script configuration
# ============================================================================

VERBOSE = True

# ============================================================================
# Shared code
# ============================================================================

class PageFormatError(Exception):

    def __init__(self, value):
         self.value = value

    def __str__(self):
        return repr(self.value)

class Page:

    class LinkParser(sgmllib.SGMLParser):

        def __init__(self, verbose=0):
            sgmllib.SGMLParser.__init__(self, verbose)
            self.current_link = None
            self.link_map = {}

        def parse(self, s):
            self.feed(s)
            self.close()

        def start_a(self, attributes):
            for name, value in attributes:
                if name == "href":
                    self.current_link = value

        def handle_data(self, data):
            if self.current_link:
                if not self.link_map.has_key(data):
                    self.link_map[data] = []
                self.link_map[data].append(self.current_link)

        def end_a(self):
            self.current_link = None

    def __init__(self, url):
        self.url = url
        self._link_map = None

    def retrieve(self):
        connection = urllib.urlopen(self.url)
        page = connection.read()
        connection.close()
        parser = self.LinkParser()
        parser.parse(page)
        self._link_map = parser.link_map

    def getSingleLink(self, link_text, allow_none = False):
        if not self._link_map:
            self.retrieve()

        if not self._link_map.has_key(link_text):
            if(allow_none): return None
            raise PageFormatError("Page format error: No link with text " \
                                  "\'%s\' found (requested with allow_none = " \
                                  "False)." % link_text)

        links = self._link_map[link_text]
        if len(links) > 1:
            raise PageFormatError("Page format error: Requested a single link "\
                                  "with text \'%s\'. Found multiple " \
                                  "candidates: %s" % (link_text, repr(links)))

        return links[0]

    def getLinks(self, link_text):
        if not self._link_map:
            self.retrieve()

        if not self._link_map.has_key(link_text):
            return []

        return self._link_map[link_text]

def hlpr_write_note_file(path, note):
    with open(path, 'w') as f:
        f.write(note)

# ============================================================================
# f-droid.org
# ============================================================================

class FDroid_Params:
    MAIN_PAGE_URL = "http://f-droid.org/repository/browse"
    NEXT_LINK_TEXT = "next"
    APP_PAGE_LINK_TEXT = "Details..."
    APK_LINK_TEXT = "download apk"
    SRC_LINK_TEXT = "source tarball"
    DL_SUBDIR = "f-droid"

class FDroidApp:
    def __init__(self, app_name, apk_url, src_url):
        self.name = app_name
        self.apk = apk_url
        self.src = src_url

class FDroidCrawler:

    def __init__(self, root_dir):
        self.root_dir = root_dir
        return

    def _get_app_pages(self):
        app_pages = []
        current_link = FDroid_Params.MAIN_PAGE_URL
        while current_link:
            page = Page(current_link)
            page.retrieve()
            app_pages[len(app_pages):] = \
                                page.getLinks(FDroid_Params.APP_PAGE_LINK_TEXT)
            current_link = page.getSingleLink(FDroid_Params.NEXT_LINK_TEXT,
                                              allow_none = True)
        return app_pages

    def _app_name_from_url(self, url):
        query_str = urlparse.urlparse(url).query
        fdid = urlparse.parse_qs(query_str)['fdid']
        if not fdid or len(fdid) != 1:
            return "Unknown"
        else:
            return fdid[0]

    def _parse_app_page(self, url):
        """
        Returns (apk, src) the urls for downloading the APK file and the source
        for the app.
        """
        app_name = self._app_name_from_url(url)
        page = Page(url)
        page.retrieve()

        apk_links = page.getLinks(FDroid_Params.APK_LINK_TEXT)
        if len(apk_links) == 0:
            if(VERBOSE):
                print "WARNING: No APK file found for application %s on the " \
                      "f-droid marketplace." % app_name
            apk_links.append(None)

        src_links = page.getLinks(FDroid_Params.SRC_LINK_TEXT)
        if len(src_links) == 0:
            if(VERBOSE):
                print "WARNING: No source code found for application %s on " \
                      "the f-droid marketplace." % app_name
            src_links.append(None)

        return FDroidApp(app_name, apk_links[0], src_links[0])

    def crawl(self):
        fdroid_dl_dir = os.path.join(self.root_dir, FDroid_Params.DL_SUBDIR)
        app_page_urls = self._get_app_pages()
        for url in app_page_urls:
            app = self._parse_app_page(url)
            app_dl_dir = os.path.join(fdroid_dl_dir, app.name)

            apk_dir = os.path.join(app_dl_dir, "apk")
            os.makedirs(apk_dir)
            if(app.apk != None):
                apk_file = os.path.join(apk_dir, app.apk.split('/')[-1])
                urllib.urlretrieve(app.apk, apk_file)
            else:
                with open(os.path.join(apk_dir, "ERROR.txt"), 'w') as f:
                    f.write("The APK file for this app could not be " \
                            "automatically retrieved from the f-droid " \
                            "marketplace.\n\n" \
                            "Link to the app page: %s\n" % url)

            src_dir = os.path.join(app_dl_dir, "src")
            os.makedirs(src_dir)
            if(app.src != None):
                src_file = os.path.join(src_dir, app.src.split('/')[-1])
                urllib.urlretrieve(app.src, src_file)
                ret_code = subprocess.call(['tar', '-xf', src_file,
                                            '-C', src_dir])
                if ret_code != 0 and VERBOSE:
                    print "WARNING: Unable to extract source code archive %s " \
                          "of app %s downloaded from the f-droid marketplace." \
                          % (src_file, app_name)
            else:
                with open(os.path.join(src_dir, "ERROR.txt"), 'w') as f:
                    f.write("The source for this app could not be " \
                            "automatically retrieved from the f-droid " \
                            "marketplace.\n\n" \
                            "Link to the app page: %s\n" % url)


# ============================================================================
# Run script
# ============================================================================

def main():
    if(len(sys.argv) != 2):
        print "USAGE: crawly.py path_to_apps_directory"
        sys.exit(2)
    root_dir = sys.argv[1]
    crawlers = [FDroidCrawler(root_dir)]
    for crawler in crawlers:
        crawler.crawl()


if __name__ == "__main__":
    main()
