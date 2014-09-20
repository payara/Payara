/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.enterprise.tools.verifier.tests.web.elements;

/** 
 * Mime type element contains an legal mime type within web application test.
 *    i.e. text/plain
 *  Define all mime types here
 *   taken from  -
 *     ftp://ftp.isi.edu/in-notes/iana/assignments/media-types/media-types
 */
public interface MimeTypes {

    // MEDIA TYPES
    //[RFC2045,RFC2046] specifies that Content Types, Content Subtypes, Character
    //Sets, Access Types, and Conversion values for MIME mail will be
    //assigned and listed by the IANA.


    //Content Types and Subtypes
    //--------------------------

    //Type            Subtype         Description                 Reference
    //----            -------         -----------                 ---------

    //text            
    static String [] text = {
	"plain", //                               [RFC2646,RFC2046]
	"richtext", //                            [RFC2045,RFC2046]
	"enriched", //                                    [RFC1896]
	"tab-separated-values", //                   [Paul Lindner]
	"html", //                                        [RFC1866]
	"sgml", //                                        [RFC1874]
	"vnd.latex-z", //                                   [Lubos]
	"vnd.fmi.flexstor", //                             [Hurtta]
	"uri-list", //				     [Daniel]
	"vnd.abc", //					      [Allen]
	"rfc822-headers", //                              [RFC1892]
	"vnd.in3d.3dml", //				     [Powers]
	"prs.lines.tag", //				      [Lines]
	"vnd.in3d.spot", //                                [Powers]
	"css", //                                         [RFC2318]
	"xml", //                                         [RFC2376]
	"rtf", //					    [Lindner]
	"directory", //                                   [RFC2425]
	"calendar", //                                    [RFC2445]
	"vnd.wap.wml", //				      [Stark]
	"vnd.wap.wmlscript", //			      [Stark]
	"vnd.motorola.reflex", //          		     [Patton]
	"vnd.fly", //					     [Gurney]
        "javascript", //                  [ GlassFish default-web.xml] 
        "vnd.sun.j2me.app-descriptor", // [ GlassFish default-web.xml]
        "vnd.wap.wmls", //                [ GlassFish default-web.xml]
        "x-server-parsed-html" //         [ GlassFish default-web.xml]
    };


    static String [] multipart = {
	"mixed", //                               [RFC2045,RFC2046]
	"alternative", //                         [RFC2045,RFC2046]
	"digest", //                              [RFC2045,RFC2046]
	"parallel", //                            [RFC2045,RFC2046]
	"appledouble", //                [MacMime,Patrik Faltstrom]
	"header-set", //                             [Dave Crocker]
	"form-data", //                                   [RFC2388]
	"related", //					    [RFC2387]
	"report", //                                      [RFC1892]
	"voice-message", //                       [RFC2421,RFC2423]
	"signed", //                                      [RFC1847]
	"encrypted", //                                   [RFC1847]
	"byteranges" //                                  [RFC2068]
    };

    static String [] message = {
	"rfc822", //                              [RFC2045,RFC2046]
	"partial", //                             [RFC2045,RFC2046]
	"external-body", //                       [RFC2045,RFC2046]
	"news", //                        [RFC 1036, Henry Spencer]
	"http", //                                        [RFC2616]
	"delivery-status", //                             [RFC1894]
	"disposition-notification", //                    [RFC2298]
	"s-http" //                                      [RFC2660]
    };

    static String [] application = {
	"octet-stream", //                        [RFC2045,RFC2046]
	"postscript", //                          [RFC2045,RFC2046]
	"oda", //                                 [RFC2045,RFC2046]
	"atomicmail", //                    [atomicmail,Borenstein]
	"andrew-inset", //                [andrew-inset,Borenstein]
	"slate", //                           [slate,terry crowley]
	"wita", //              [Wang Info Transfer,Larry Campbell]
	"dec-dx", //            [Digital Doc Trans, Larry Campbell]
	"dca-rft", //        [IBM Doc Content Arch, Larry Campbell]
	"activemessage", //                          [Ehud Shapiro]
	"rtf", //                                    [Paul Lindner]
	"applefile", //                  [MacMime,Patrik Faltstrom]
	"mac-binhex40", //               [MacMime,Patrik Faltstrom]
	"news-message-id", //              [RFC1036, Henry Spencer]
	"news-transmission", //            [RFC1036, Henry Spencer]
	"wordperfect5.1", //                         [Paul Lindner]
	"pdf", //                                    [Paul Lindner]
	"zip", //                                    [Paul Lindner]
	"macwriteii", //                             [Paul Lindner]
	"msword", //                                 [Paul Lindner]
	"remote-printing", //                        [RFC1486,Rose]
	"mathematica", //                             [Van Nostern]
	"cybercash", //                                  [Eastlake]
	"commonground", //                                 [Glazer]
	"iges", //                                          [Parks]
	"riscos", //                                        [Smith]
	"eshop", //                                          [Katz]
	"x400-bp", //                                     [RFC1494]
	"sgml", //                                        [RFC1874]
	"cals-1840", //                                   [RFC1895]
	"pgp-encrypted", //                               [RFC2015]
	"pgp-signature", //                               [RFC2015]
	"pgp-keys", //                                    [RFC2015]
	"vnd.framemaker", //                               [Wexler]
	"vnd.mif", //                                      [Wexler]
	"vnd.ms-excel", //                                   [Gill]
	"vnd.ms-powerpoint", //                              [Gill]
	"vnd.ms-project", //                                 [Gill]
	"vnd.ms-works", //                                   [Gill]
	"vnd.ms-tnef", //                                    [Gill]
	"vnd.svd", //                                      [Becker]
	"vnd.music-niff", //                               [Butler]
	"vnd.ms-artgalry", //                             [Slawson]
	"vnd.truedoc", //                                   [Chase]
	"vnd.koan", //                                       [Cole]
	"vnd.street-stream", //                            [Levitt]
	"vnd.fdf", //                                      [Zilles]
	"set-payment-initiation", //                       [Korver]
	"set-payment", //                                  [Korver]
	"set-registration-initiation", //                  [Korver]
	"set-registration", //                             [Korver]
	"vnd.seemail", //                                    [Webb]
	"vnd.businessobjects", //                         [Imoucha]
	"vnd.meridian-slingshot", //                        [Wedel]
	"vnd.xara", //                                 [Matthewman]
	"sgml-open-catalog", //                            [Grosso]
	"vnd.rapid", //                                   [Szekely]
	"vnd.enliven", //                              [Santinelli]
	"vnd.japannet-registration-wakeup", //              [Fujii]
	"vnd.japannet-verification-wakeup", //              [Fujii]
	"vnd.japannet-payment-wakeup", //                   [Fujii]
	"vnd.japannet-directory-service", //                [Fujii]
	"vnd.intertrust.digibox", //                    [Tomasello]
	"vnd.intertrust.nncp", //                       [Tomasello]
	"prs.alvestrand.titrax-sheet", //              [Alvestrand]
	"vnd.noblenet-web", //			    [Solomon]
	"vnd.noblenet-sealer", //                         [Solomon]
	"vnd.noblenet-directory", //			    [Solomon]
	"prs.nprend", //				    [Doggett]
	"vnd.webturbo", //				      [Rehem]
	"hyperstudio", //				     [Domino]
	"vnd.shana.informed.formtemplate", //		    [Selzler]
	"vnd.shana.informed.formdata", //		    [Selzler] 
	"vnd.shana.informed.package", //		    [Selzler]
	"vnd.shana.informed.interchange", //	 	    [Selzler]
	"vnd.$commerce_battelle", //			  [Applebaum]
	"vnd.osa.netdeploy", //			       [Klos]
	"vnd.ibm.MiniPay", //				   [Herzberg]
	"vnd.japannet-jpnstore-wakeup", //		  [Yoshitake]
	"vnd.japannet-setstore-wakeup", //              [Yoshitake]
	"vnd.japannet-verification", //		  [Yoshitake]
	"vnd.japannet-registration", //		  [Yoshitake]
	"vnd.hp-HPGL", //				  [Pentecost]
	"vnd.hp-PCL", //				  [Pentecost]
	"vnd.hp-PCLXL", //				  [Pentecost]
	"vnd.musician", //				      [Adams]
	"vnd.FloGraphIt", //				   [Floersch]
	"vnd.intercon.formnet", //                          [Gurak]
	"vemmi", //                                       [RFC2122]
	"vnd.ms-asf", //				 [Fleischman]
	"vnd.ecdis-update", //		       [Buettgenbach]
	"vnd.powerbuilder6", //			        [Guy]
	"vnd.powerbuilder6-s", //				[Guy]
	"vnd.lotus-wordpro", //                      [Wattenberger]
	"vnd.lotus-approach", //                     [Wattenberger]
	"vnd.lotus-1-2-3", //                        [Wattenberger]
	"vnd.lotus-organizer", //                    [Wattenberger]
	"vnd.lotus-screencam", //                    [Wattenberger]
	"vnd.lotus-freelance", //		       [Wattenberger]
	"vnd.fujitsu.oasys", //			    [Togashi]
	"vnd.fujitsu.oasys2", //			    [Togashi]
	"vnd.swiftview-ics", //			    [Widener]
	"vnd.dna", //					     [Searcy]
	"prs.cww", //				     [Rungchavalnont]
	"vnd.wt.stf", //                                   [Wohler]
	"vnd.dxr", //				              [Duffy]
	"vnd.mitsubishi.misty-guard.trustweb", //          [Tanaka]
	"vnd.ibm.modcap", //				   [Hohensee]
	"vnd.acucobol", //                                  [Lubin]
	"vnd.fujitsu.oasys3", //                         [Okudaira]
	"marc", //                                        [RFC2220]
	"vnd.fujitsu.oasysprs", //                          [Ogita]
	"vnd.fujitsu.oasysgp", //			   [Sugimoto]
	"vnd.visio", //                                    [Sandal]
	"vnd.netfpx", //                                     [Mutz]
	"vnd.audiograph", //		           	 [Slusanschi]
	"vnd.epson.salt", //				   [Nagatomo]
	"vnd.3M.Post-it-Notes", //			    [O'Brien]
	"vnd.novadigm.EDX", //                            [Swenson]
	"vnd.novadigm.EXT", //                            [Swenson]
	"vnd.novadigm.EDM", //			    [Swenson]
	"vnd.claymore", //				    [Simpson]
	"vnd.comsocaller", //				   [Dellutri]
	"pkcs7-mime", //                                  [RFC2311]
	"pkcs7-signature", //                             [RFC2311]
	"pkcs10", //                                      [RFC2311]
	"vnd.yellowriver-custom-menu", //		     [Yellow]
	"vnd.ecowin.chart", //			     [Olsson]
	"vnd.ecowin.series", //			     [Olsson]
	"vnd.ecowin.filerequest", //                       [Olsson]
	"vnd.ecowin.fileupdate", //                        [Olsson]
	"vnd.ecowin.seriesrequest", //                     [Olsson]
	"vnd.ecowin.seriesupdate", //                      [Olsson]
	"EDIFACT", //					    [RFC1767]
	"EDI-X12", //					    [RFC1767]
	"EDI-Consent", //				    [RFC1767]
	"vnd.wrq-hp3000-labelled", //			    [Bartram]
	"vnd.minisoft-hp3000-save", //		    [Bartram]
	"vnd.ffsns", //				   [Holstage]
	"vnd.hp-hps", //                                   [Aubrey]
	"vnd.fujixerox.docuworks", //                     [Taguchi]
	"xml", //                                         [RFC2376]
	"vnd.anser-web-funds-transfer-initiation", //        [Mori]
	"vnd.anser-web-certificate-issue-initiation", //     [Mori]
	"vnd.is-xpr", //				  [Natarajan]
	"vnd.intu.qbo", //			         [Scratchley]
	"vnd.publishare-delta-tree", //		   [Ben-Kiki]
	"vnd.cybank", //				     [Helmee]
	"batch-SMTP", //                                  [RFC2442]
	"vnd.uplanet.alert", //			     [Martin]
	"vnd.uplanet.cacheop", //			     [Martin]
	"vnd.uplanet.list", //			     [Martin]
	"vnd.uplanet.listcmd", //			     [Martin]
	"vnd.uplanet.channel", //			     [Martin]
	"vnd.uplanet.bearer-choice", //		     [Martin]
	"vnd.uplanet.signal", //			     [Martin]
	"vnd.uplanet.alert-wbxml", //			     [Martin]
	"vnd.uplanet.cacheop-wbmxl", //		     [Martin]
	"vnd.uplanet.list-wbxml", //			     [Martin]
	"vnd.uplanet.listcmd-wbxml", //		     [Martin]
	"vnd.uplanet.channel-wbxml", //		     [Martin]
	"vnd.uplanet.bearer-choice-wbxml", //		     [Martin]
	"vnd.epson.quickanime", //                             [Gu]
	"vnd.commonspace", //				   [Chandhok]
	"vnd.fut-misnet", //				  [Pruulmann]
	"vnd.xfdl", //				    [Manning]
	"vnd.intu.qfx", //                             [Scratchley]
	"vnd.epson.ssf", //				    [Hoshina]
	"vnd.epson.msf", //				    [Hoshina]
	"vnd.powerbuilder7", //			     [Shilts]	
	"vnd.powerbuilder7-s", //			     [Shilts]
	"vnd.lotus-notes", //				    [Laramie]
	"pkixcmp", //                                     [RFC2510]
	"vnd.wap.wmlc", //				      [Stark]
	"vnd.wap.wmlscriptc", //			      [Stark]
	"vnd.motorola.flexsuite", //			     [Patton]
	"vnd.wap.wbxml", //				      [Stark] 
	"vnd.motorola.flexsuite.wem", //   		     [Patton]
	"vnd.motorola.flexsuite.kmr", //   		     [Patton]
	"vnd.motorola.flexsuite.adsi", //   		     [Patton]
	"vnd.motorola.flexsuite.fis", //   		     [Patton]
	"vnd.motorola.flexsuite.gotap", // 		     [Patton]
	"vnd.motorola.flexsuite.ttc", //   		     [Patton]
	"vnd.ufdl", //				    [Manning]
	"vnd.accpac.simply.imp", //			       [Leow]
	"vnd.accpac.simply.aso", //			       [Leow]
	"vnd.vcx", //					 [T.Sugimoto]  
	"ipp", //                                         [RFC2565]
	"ocsp-request", //                                [RFC2560]
	"ocsp-response", //                               [RFC2560]
	"vnd.previewsystems.box", //			 [Smolgovsky]
	"vnd.mediastation.cdkey", //			     [Flurry]
	"vnd.pg.format", //				    [Gandert]
	"vnd.pg.osasli", //				    [Gandert]
	"vnd.hp-hpid", //				      [Gupta]
	"pkix-cert", //                                   [RFC2585]
	"pkix-crl", //                                    [RFC2585]
	"vnd.Mobius.TXF", //				   [Kabayama]
	"vnd.Mobius.PLC", //				   [Kabayama]
	"vnd.Mobius.DIS", //				   [Kabayama]
	"vnd.Mobius.DAF", //				   [Kabayama]
	"vnd.Mobius.MSL", //				   [Kabayama]
	"vnd.cups-raster", //				      [Sweet]
	"vnd.cups-postscript", //			      [Sweet]
	"vnd.cups-raw", //		   		      [Sweet]
	"index", //                                       [RFC2652]
	"index.cmd", //                                   [RFC2652]
	"index.response", //                              [RFC2652]
	"index.obj", //                                   [RFC2652]
	"index.vnd", //                                   [RFC2652]
	"vnd.triscape.mxs", //			   [Simonoff]
	"vnd.powerbuilder75", //			     [Shilts]
	"vnd.powerbuilder75-s", //			     [Shilts]
	"vnd.dpgraph", //				     [Parker]
	"http", //					    [RFC2616]
	"sdp", //					    [RFC2327]
        "java",  //                      [ GlassFish default-web.xml]
        "java-archive", //               [ GlassFish default-web.xml]
        "vnd.rn-realmedia", //           [ GlassFish default-web.xml]
        "xslt+xml", //                   [ GlassFish default-web.xml]
        "powerpoint", //                 [ GlassFish default-web.xml]
        "x-visio", //                    [ GlassFish default-web.xml]
        "vnd.mozilla.xul+xml", //        [ GlassFish default-web.xml]
        "x-stuffit", //                  [ GlassFish default-web.xml]
        "xml-dtd", //                                       [RFC3023]
        "xhtml+xml", //                                     [RFC3236] 
        "mathml+xml", //                                    [RFC3023]
        "ogg", //                                           [RFC3534]
        "voicexml+xml", //                                  [RFC4267]
        "rdf+xml" //                                        [RFC3023]
    };

    static String [] image = {
	"jpeg", //                                [RFC2045,RFC2046]
	"gif", //                                 [RFC2045,RFC2046]
	"ief", //             Image Exchange Format       [RFC1314]
	"g3fax", //                                       [RFC1494]
	"tiff", //            Tag Image File Format       [RFC2302]
	"cgm", //		Computer Graphics Metafile  [Francis]
	"naplps", //                                       [Ferber]
	"vnd.dwg", //                                      [Moline]
	"vnd.svf", //                                      [Moline]
	"vnd.dxf", //                                      [Moline]
	"png", //                                 [Randers-Pehrson]
	"vnd.fpx", //                                     [Spencer]
	"vnd.net-fpx", //                                 [Spencer]
	"vnd.xiff", //				    [SMartin]
	"prs.btif", //				      [Simon]
	"vnd.fastbidsheet", //			     [Becker]	
	"vnd.wap.wbmp", //				      [Stark]
	"prs.pti", //					       [Laun]	
	"vnd.cns.inf2", //				 [McLaughlin] 
	"vnd.mix", //					      [Reddy]
        "bmp", //                        [ GlassFish default-web.xml]
        "pict", //                       [ GlassFish default-web.xml]
        "svg+xml" //                                        [RFC3023]
    };

    static String [] audio = {
	"basic", //                               [RFC2045,RFC2046]
	"32kadpcm", //                            [RFC2421,RFC2422]
	"vnd.qcelp", //                                 [Lundblade]
	"vnd.digital-winds", //			    [Strazds]
	"vnd.lucent.voice", //			  [Vaudreuil]
	"vnd.octel.sbc", //                             [Vaudreuil]
	"vnd.rhetorex.32kadpcm", //                     [Vaudreuil]
	"vnd.vmx.cvsd", //                              [Vaudreuil]
	"vnd.nortel.vbk", //			            [Parsons]
	"vnd.cns.anp1", //				 [McLaughlin]
	"vnd.cns.inf1", //				 [McLaughlin]
	"L16" //                                         [RFC2586]
    };

    static String [] video = {
	"mpeg", //                                [RFC2045,RFC2046]
	"quicktime", //                              [Paul Lindner]
	"vnd.vivo", //                                      [Wolfe]
	"vnd.motorola.video", //                          [McGinty]
	"vnd.motorola.videop", //                         [McGinty]
        "mpeg2" //                     [ GlassFish default-web.xml]
    };

    static String [] model = { //                                            [RFC2077]
	"iges", //                                          [Parks]
	"vrml", //                                        [RFC2077]
	"mesh", //                                        [RFC2077]
	"vnd.dwf", //					      [Pratt]
	"vnd.gtw", //					      [Ozaki]
	"vnd.flatland.3dml" //			     [Powers]
    };

    /* The "media-types" directory contains a subdirectory for each content
       type and each of those directories contains a file for each content
       subtype.

       |-application-
       |-audio-------
       |-image-------
       |-media-types-|-message-----
       |-model-------
       |-multipart---
       |-text--------
       |-video-------

       URL = ftp://ftp.isi.edu/in-notes/iana/assignments/media-types


       Character Sets
       --------------

       All of the character sets listed the section on Character Sets are
       registered for use with MIME as MIME Character Sets.  The
       correspondance between the few character sets listed in the MIME
       specifications [RFC2045,RFC2046] and the list in that section are:

       Type           Description				    Reference
       ----           -----------                                  ---------
       US-ASCII       see ANSI_X3.4-1968 below             [RFC2045,RFC2046]
       ISO-8859-1     see ISO_8859-1:1987 below            [RFC2045,RFC2046]
       ISO-8859-2     see ISO_8859-2:1987 below            [RFC2045,RFC2046]
       ISO-8859-3     see ISO_8859-3:1988 below            [RFC2045,RFC2046]
       ISO-8859-4     see ISO_8859-4:1988 below            [RFC2045,RFC2046]
       ISO-8859-5     see ISO_8859-5:1988 below            [RFC2045,RFC2046]
       ISO-8859-6     see ISO_8859-6:1987 below            [RFC2045,RFC2046]
       ISO-8859-7     see ISO_8859-7:1987 below            [RFC2045,RFC2046]
       ISO-8859-8     see ISO_8859-8:1988 below            [RFC2045,RFC2046]
       ISO-8859-9     see ISO_8859-9:1989 below            [RFC2045,RFC2046]

       Access Types
       ------------

       Type           Description				    Reference
       ----           -----------				    ---------
       FTP			                            [RFC2045,RFC2046]
       ANON-FTP	                                    [RFC2045,RFC2046]
       TFTP		                                    [RFC2045,RFC2046]
       AFS		                                    [RFC2045,RFC2046]
       LOCAL-FILE	                                    [RFC2045,RFC2046]
       MAIL-SERVER	                                    [RFC2045,RFC2046]
       content-id                                                  [RFC1873]


       Conversion Values
       -----------------

       Conversion values or Content Transfer Encodings.

       Type           Description				    Reference
       ----           -----------				    ---------
       7BIT                                                [RFC2045,RFC2046]
       8BIT                                                [RFC2045,RFC2046]
       BASE64		 		                    [RFC2045,RFC2046]
       BINARY                                              [RFC2045,RFC2046]
       QUOTED-PRINTABLE                                    [RFC2045,RFC2046]


       MIME / X.400 MAPPING TABLES

       MIME to X.400 Table

       MIME content-type          X.400 Body Part             Reference
       -----------------          ------------------          ---------
       text/plain
       charset=us-ascii         ia5-text                     [RFC1494]
       charset=iso-8859-x       EBP - GeneralText            [RFC1494]
       text/richtext              no mapping defined           [RFC1494]
       application/oda            EBP - ODA                    [RFC1494]
       application/octet-stream   bilaterally-defined          [RFC1494]
       application/postscript     EBP - mime-postscript-body   [RFC1494]
       image/g3fax                g3-facsimile                 [RFC1494]
       image/jpeg                 EBP - mime-jpeg-body         [RFC1494]
       image/gif                  EBP - mime-gif-body          [RFC1494]
       audio/basic                no mapping defined           [RFC1494]
       video/mpeg                 no mapping defined           [RFC1494]

       Abbreviation: EBP - Extended Body Part


       X.400 to MIME Table


       Basic Body Parts

       X.400 Basic Body Part      MIME content-type           Reference
       ---------------------      --------------------        ---------
       ia5-text                   text/plain;charset=us-ascii [RFC1494]
       voice                      No Mapping Defined          [RFC1494]
       g3-facsimile               image/g3fax                 [RFC1494]
       g4-class1                  no mapping defined          [RFC1494]
       teletex                    no mapping defined          [RFC1494]
       videotex                   no mapping defined          [RFC1494]
       encrypted                  no mapping defined          [RFC1494]
       bilaterally-defined        application/octet-stream    [RFC1494]
       nationally-defined         no mapping defined          [RFC1494]
       externally-defined         See Extended Body Parts     [RFC1494]

       X.400 Extended Body Part  MIME content-type            Reference
       ------------------------- --------------------         ---------
       GeneralText               text/plain;charset=iso-8859-x[RFC1494] 
       ODA                       application/oda              [RFC1494]
       mime-postscript-body      application/postscript       [RFC1494]
       mime-jpeg-body            image/jpeg                   [RFC1494]
       mime-gif-body             image/gif                    [RFC1494]

       REFERENCES

       [MacMime] Work in Progress.

       [RFC1036] Horton, M., and R. Adams, "Standard for Interchange of
       USENET Messages", RFC 1036, AT&T Bell Laboratories,
       Center for Seismic Studies, December 1987.

       [RFC1494] Alvestrand, H., and S. Thompson, "Equivalences between 1988
       X.400 and RFC-822 Message Bodies", RFC 1494, SINTEF DELAB,
       Soft*Switch, Inc., August 1993.

       [RFC1563] Borenstien, N., "The text/enriched MIME content-type". RFC
       1563, Bellcore, January 1994.

       [RFC1767] Crocker, D., "MIME Encapsulation of EDI Objects". RFC 1767,
       Brandenburg Consulting, March 1995.

       [RFC1866] Berners-Lee, T., and D. Connolly, "Hypertext Markup Language
       - 2.0", RFC 1866, MIT/W3C, November 1995.

       [RFC1873] Levinson, E., "Message/External-Body Content-ID Access
       Type", RFC 1873, Accurate Information Systems, Inc. December
       1995.

       [RFC1874] Levinson, E., "SGML Media Types", RFC 1874, Accurate
       Information Systems, Inc. December 1995.

       [RFC1892] Vaudreuil, G., "The Multipart/Report Content Type for the
       Reporting of Mail System Administrative Messages", RFC 1892,
       Octel Network Services, January 1996.

       [RFC1894] Moore, K. and G. Vaudreuil, "An Extensible Message Format
       for Delivery Status Notifications", RFC 1894, University of
       Tennessee, Octel Network Services, January 1996.

       [RFC1895] Levinson, E., "The Application/CALS-1840 Content Type", RFC
       1895, Accurate Information Systems, February 1996.

       [RFC1896] Resnick, P., and A. Walker, "The Text/Enriched MIME Content
       Type", RFC 1896, Qualcomm, Intercon, February 1996.

       [RFC1945] Berners-Lee, Y., R. Feilding, and H.Frystyk, "Hypertext
       Transfer Protocol -- HTTP/1.0", RFC 1945. MIT/LCS, UC
       Irvine, MIT/LCS, May 1996.

       [RFC2045] Freed, N., and N. Borenstein, "Multipurpose Internet Mail
       Extensions (MIME) Part One: Format of Internet Message
       Bodies", RFC 2045, November 1996.

       [RFC2046] Freed, N., and N. Borenstein, "Multipurpose Internet Mail
       Extensions (MIME) Part Two: Media Types", RFC 2046, November
       1996. 

       [RFC2077] Nelson, S., C. Parks, and Mitra, "The Model Primary Content
       Type for Multipurpose Internet Mail Extensions", RFC 2077,
       LLNL, NIST, WorldMaker, January 1997.

       [RFC2122] Mavrakis, D., Layec, H., and K. Kartmann, "VEMMI URL
       Specification", RFC 2122, Monaco Telematique MC-TEL,
       ETSI, Telecommunication+Multimedia, March 1997. 

       [RFC2220] Guenther, R., "The Application/MARC Content-type", RFC 2220,
       Library of Congress, Network Devt. & MARC Standards Office,
       October 1997. 

       [RFC2298] Fajman, R., "An Extensible Message Format for Message
       Disposition Notifications", RFC 2298, March 1998.

       [RFC2302] Parsons, G., et. al., "Tag Image File Format (TIFF) -
       image/tiff", RFC 2302, March 1998.

       [RFC2311] Dusse, S., et. al., "S/MIME Version 2 Message Specification,
       RFC 2311, March 1998.

       [RFC2318] Lie, H., Bos, B., and C. Lilley, "The text/css Media Type", 
       RFC 2318, March 1998.

       [RFC2327] Handley, M., and V. Jacobson, "SDP: Session Description
       Protocol", RFC 2327, April 1999.

       [RFC2376] Whitehead, E., and M. Murata, "XML Media Types", July 1998.

       [RFC2387] Levinson, E., "The MIME Multipart/Related Content-type", RFC
       2387, XIson Inc, August 1998.

       [RFC2388] Masinter, L., "Form-based File Upload in HTML",
       RFC 2388, Xerox Corporation, August 1998.

       [RFC2421] Vaudreuil, G., and G. Parsons, "Voice Profile for Internet
       Mail - version 2", RFC 2421, September 1998.

       [RFC2422] Vaudreuil, G., and G. Parsons, "Toll Quality Voice - 32
       kbit/s ADPCM MIME Sub-type Registration", RFC 2422,
       September 1998.

       [RFC2423] Vaudreuil, G., and G. Parsons, "VPIM Voice Message MIME
       Sub-type Registration", RFC 2423, September 1998.

       [RFC2425] Howes, T., Smith, M., and F. Dawson, "A MIME Content-Type
       for Directory Information", RFC 2425, September 1998.

       [RFC2442] Freed, N., Newman, D., Belissent, J. and M. Hoy, "The
       Batch SMTP Media Type", RFC 2442, November 1998.

       [RFC2445] Dawson, F., and D. Stenerson, "Internet Calendaring and
       Scheduling Core Object Specification (iCalendar)", RFC 2445,
       November 1998.

       [RFC2510] Adams, C., and S. Farrell, "Internet X.509 Public Key
       Infrastructure Certificate Management Protocols", RFC 2510,
       March 1999.

       [RFC2560] Myers, M., Ankney, R., Malpani, A., Galperin, S., and C.
       Adams, "X.509 Internet Public Key Infrastructure Online
       Certificate Status Protocol - OCSP", RFC 2560, June 1999.

       [RFC2565] Herriot, R., Editor, Butler, S., Moore, P., and R. Turner,
       "Internet Printing Protocol/1.0: Encoding and Transport",
       RFC 2565, April 1999.

       [RFC2585] Housley, R. and P. Hoffman, "Internet X.509 Public Key
       Infrastructure Operational Protocols: FTP and HTTP", 
       RFC 2585, May 1999.

       [RFC2586] Salsman, J and H. Alvestrand, "The Audio/L16 MIME content
       type", RFC 2586, May 1999.

       [RFC2616] Fielding, R., et. al., "Hypertext Transfer Protocol --
       HTTP/1.1", RFC 2616, June 1999.

       [RFC2652] Allen, J., and M. Mealling, "MIME Object Definitions for the
       Common Indexing Protocol (CIP)", RFC 2652, August 1999. 


       PEOPLE

       [Adams] Greg Adams <gadams@waynesworld.ucsd.edu>, March 1997.

       [Allen] Steve Allen <sla@ucolick.org>, September 1997.

       [Alvestrand] Harald T. Alvestrand <Harald.T.Alvestrand@uninett.no>,
       January 1997.

       [Applebaum] David Applebaum <applebau@battelle.org>, February 1997.

       [Aubrey] Steve Aubrey <steve_aubrey@hp.com>, July 1998.

       [Bartram] Chris Bartram <RCB@3k.com>, May 1998.

       [Becker] Scott Becker, <scottb@bxwa.com>, April 1996, October 1998.

       [Ben-Kiki] Oren Ben-Kiki, <oren@capella.co.il>, October 1998.

       [Berners-Lee] Tim Berners-Lee, <timbl@w3.org>, May 1996.

       [Borenstein] Nathaniel Borenstein, <NSB@bellcore.com>, April 1994.

       [Buettgenbach] Gert Buettgenbach, <bue@sevencs.com>, May 1997.

       [Butler] Tim Butler, <tim@its.bldrdoc.gov>, April 1996.

       [Larry Campbell]

       [Chandhok] Ravinder Chandhok, <chandhok@within.com>, December 1998.

       [Chase] Brad Chase, <brad_chase@bitstream.com>, May 1996.

       [Cole] Pete Cole, <pcole@sseyod.demon.co.uk>, June 1996.

       [Dave Crocker]  Dave Crocker <dcrocker@mordor.stanford.edu>

       [Terry Crowley]

       [Daniel] Ron Daniel, Jr. <rdaniel@lanl.gov>, June 1997.

       [Dellutri] Steve Dellutri <sdellutri@cosmocom.com>, March 1998.

       [Doggett] Jay Doggett, <jdoggett@tiac.net>, February 1997.

       [Domino] Michael Domino, <michael-@ultranet.com>, February 1997.

       [Duffy] Michael Duffy, <miked@psiaustin.com>, September 1997.

       [Eastlake] Donald E. Eastlake 3rd, dee@cybercash.com, April 1995.

       [Faltstrom] Patrik Faltstrom <paf@nada.kth.se>

       [Fleischman] Eric Fleischman <ericfl@MICROSOFT.com>, April 1997.

       [Floersch] Dick Floersch <floersch@echo.sound.net>, March 1997.

       [Flurry] Henry Flurry <henryf@mediastation.com>, April 1999.

       [Francis] Alan Francis, A.H.Francis@open.ac.uk, December 1995.

       [Fujii] Kiyofusa Fujii <kfujii@japannet.or.jp>, February 1997.

       [Gandert] April Gandert <gandert.am@pg.com>, April 1999.

       [Gill] Sukvinder S. Gill, <sukvg@microsoft.com>, April 1996.

       [Glazer] David Glazer, <dglazer@best.com>, April 1995.

       [Gu] Yu Gu, <guyu@rd.oda.epson.co.jp>, December 1998.

       [Gupta] Aloke Gupta <Aloke_Gupta@ex.cv.hp.com>, April 1999.

       [Gurak] Tom Gurak, <assoc@intercon.roc.servtech.com>, March 1997.

       [Gurney] John-Mark Gurney <jmg@flyidea.com>, August 1999.

       [Guy] David Guy, <dguy@powersoft.com>, June 1997.

       [Helmee] Nor Helmee, <helmee@my.cybank.net>, November 1998.

       [Herzberg] Amir Herzberg, <amirh@haifa.vnet.ibm.com>, February 1997.

       [Hohensee] Reinhard Hohensee <rhohensee@VNET.IBM.COM>, September 1997.

       [Holstage] Mary Holstage <holstege@firstfloor.com>, May 1998.

       [Hoshina] Shoji Hoshina <Hoshina.Shoji@exc.epson.co.jp>, January 1999.

       [Hurtta] Kari E. Hurtta <flexstor@ozone.FMI.FI>

       [Imoucha] Philippe Imoucha <pimoucha@businessobjects.com>, October 1996.

       [Katz] Steve Katz, <skatz@eshop.com>, June 1995.

       [Klos] Steven Klos, <stevek@osa.com>, February 1997.

       [Korver] Brian Korver <briank@terisa.com>, October 1996.

       [Laramie] Michael Laramie <laramiem@btv.ibm.com>, February 1999.

       [Laun] Juern Laun <juern.laun@gmx.de>, April 1999.

       [Leow] Steve Leow <Leost01@accpac.com>, April 1999.

       [Levitt] Glenn Levitt <streetd1@ix.netcom.com>, October 1996.

       [Lines] John Lines <john@paladin.demon.co.uk>, January 1998.

       [Lubin] Dovid Lubin <dovid@acucobol.com>, October 1997.

       [Lubos] Mikusiak Lubos <lmikusia@blava-s.bratisla.ingr.com>, October 1996.

       [Lundblade] Laurence Lundblade <lgl@qualcomm.com>, October 1996.

       [Manning] Dave Manning <dmanning@uwi.com>, January, March 1999.

       [Martin] Bruce Martin <iana-registrar@uplanet.com>, November 1998.

       [SMartin] Steven Martin <smartin@xis.xerox.com>, October 1997.

       [Matthewman] David Matthewman <david@xara.com>, October 1996.

       [McGinty] Tom McGinty <tmcginty@dma.isg.mot.com>

       [McLaughlin] Ann McLaughlin <amclaughlin@comversens.com>, April 1999.

       [Moline] Jodi Moline, <jodim@softsource.com>, April 1996.

       [Mori] Hiroyoshi Mori <mori@mm.rd.nttdata.co.jp>, August 1998.

       [Mutz] Andy Mutz, <andy_mutz@hp.com>, December 1997.

       [Nagatomo] Yasuhito Nagatomo <naga@rd.oda.epson.co.jp>, January 1998.

       [Natarajan] Satish Natarajan <satish@infoseek.com>, August 1998.

       [O'Brien] Michael O'Brien <meobrien1@mmm.com>, January 1998.

       [Ogita] Masumi Ogita, <ogita@oa.tfl.fujitsu.co.jp>, October 1997.

       [Okudaira] Seiji Okudaira <okudaira@candy.paso.fujitsu.co.jp>, October 1997.

       [Olsson] Thomas Olsson <thomas@vinga.se>, April 1998.

       [Ozaki] Yutaka Ozaki <yutaka_ozaki@gen.co.jp>, January 1999.

       [Paul Lindner]

       [Parker] David Parker <davidparker@davidparker.com>, August 1999.

       [Parks] Curtis Parks, <parks@eeel.nist.gov>, April 1995.

       [Parsons] Glenn Parsons <gparsons@nortelnetworks.com>, February 1999.

       [Patton] Mark Patton <fmp014@email.mot.com>, March 1999.

       [Pentecost] Bob Pentecost, <bpenteco@boi.hp.com>, March 1997.

       [Powers] Michael Powers, <powers@insideout.net>, January 1998.
       <pow@flatland.com>, January 1999.

       [Pratt] Jason Pratt, <jason.pratt@autodesk.com>, August 1997.

       [Pruulmann] Jann Pruulmann, <jaan@fut.ee>, December 1998.

       [Randers-Pehrson] Glenn Randers-Pehrson <glennrp@ARL.MIL>, October 1996.

       [Reddy] Saveen Reddy <saveenr@miscrosoft.com>, July 1999.

       [Rehem] Yaser Rehem, <yrehem@sapient.com>, February 1997.

       [Rose] Marshall Rose, <mrose@dbc.mtview.ca.us>, April 1995.

       [Rungchavalnont] Khemchart Rungchavalnont,
       <khemcr@cpu.cp.eng.chula.ac.th>, July 1997.

       [Sandal] Troy Sandal <troys@visio.com>, November 1997.

       [Santinelli] Paul Santinelli, Jr. <psantinelli@narrative.com>, October 1996.

       [Scrathcley] Greg Scratchley <greg_scratchley@intuit.com>, October 1998.

       [Searcy] Meredith Searcy, <msearcy@newmoon.com>, June 1997.

       [Shapiro] Ehud Shapiro

       [Shilts] Reed Shilts <reed.shilts@sybase.com>, February 1999, August 1999.

       [Simon] Ben Simon, <BenS@crt.com>, September 1998.

       [Simonoff] Steven Simonoff <scs@triscape.com>, August 1999.

       [Simpson] Ray Simpson <ray@cnation.com>, January 1998.

       [Slawson] Dean Slawson, <deansl@microsoft.com>, May 1996.

       [Slusanschi] Horia Cristian Slusanschi <H.C.Slusanschi@massey.ac.nz>, 
       January 1998.

       [Smith] Nick Smith, <nas@ant.co.uk>, June 1995.

       [Smolgovsky] Roman Smolgovsky <romans@previewsystems.com>, April 1999.

       [Solomon] Monty Solomon, <monty@noblenet.com>, February 1997.

       [Spencer] Marc Douglas Spencer <marcs@itc.kodak.com>, October 1996.

       [Henry Spencer]

       [Stark] Peter Stark <stark@uplanet.com>, March 1999.

       [Strazds] Armands Strazds <armands.strazds@medienhaus-bremen.de>,
       January 1999.

       [Sugimoto] Masahiko Sugimoto <sugimoto@sz.sel.fujitsu.co.jp>, October 1997.

       [T.Sugimoto] Taisuke Sugimoto <sugimototi@noanet.nttdata.co.jp> April 1999.

       [Sweet] Michael Sweet <mike@easysw.com>, July 1999.

       [Swenson] Janine Swenson <janine@novadigm.com>, January 1998.

       [Szekely] Etay Szekely <etay@emultek.co.il>, October 1996.

       [Taguchi] Yasuo Taguchi <yasuo.taguchi@fujixerox.co.jp>, July 1998.

       [Tanaka] Manabu Tanaka <mtana@iss.isl.melco.co.jp>, September 1997.

       [Togashi] Nobukazu Togashi <togashi@ai.cs.fujitsu.co.jp>, June 1997.

       [Tomasello] Luke Tomasello <luket@intertrust.com>

       [Vaudreuil] Greg Vaudreuil <gregv@lucent.com>, January 1999.

       [Wattenberger] Paul Wattenberger <Paul_Wattenberger@lotus.com>, June 1997.

       [Webb] Steve Webb <steve@wynde.com>, October 1996.

       [Wedel] Eric Wedel <ewedel@meridian-data.com>, October 1996.

       [Wexler] Mike Wexler, <mwexler@frame.com>, April 1996.

       [Widener] Glenn Widener <glennw@ndg.com>, June 1997.

       [Wohler] Bill Wohler, <wohler@newt.com>, July 1997.

       [Wolfe] John Wolfe, <John_Wolfe.VIVO@vivo.com>, April 1996.

       [Van Nostern] Gene C. Van Nostern <gene@wri.com>, February 1995.

       [Yellow] Mr. Yellow <yellowriversw@yahoo.com>, March 1998.

       [Yoshitake] Jun Yoshitake, <yositake@iss.isl.melco.co.jp>, February 1997.

       [Zilles] Steve Zilles <szilles@adobe.com>, October 1996.

       []

    */
}
