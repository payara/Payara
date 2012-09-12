/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admingui.common.servlet;

import java.io.IOException;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



/**
 *  <p>	This Servlet provides the ability to download information from the
 *	Server to the client.  It provides the ability to set the content type
 *	of the downloaded file, if not specified, it will attempt to guess
 *	based on the extension (if possible).  It requires the
 *	{@link DownloadServlet#ContentSource} of the data to download to be
 *	specified by passing in a <code>ServletRequest</code> parameter named
 *	{@link DownloadServlet#CONTENT_SOURCE_ID}.  The
 *	{@link DownloadServlet.ContentSource} provides a plugable means
 *	of obtaining data from an arbitrary source (i.e. the filesystem,
 *	generated on the fly, from some network location, etc.).  The available
 *	{@link DownloadServlet.ContentSource} implemenatations must be
 *	specified via a <code>Servlet</code> init parameter named
 *	{@link DownloadServlet#CONTENT_SOURCES}.</p>
 */
public class DownloadServlet extends HttpServlet {

    /**
     *	<p> Default Constructor.</p>
     */
    public DownloadServlet() {
	super();
    }

    /**
     *	<p> Servlet initialization method.</p>
     */
    public void init(ServletConfig config) throws ServletException {
	super.init(config);

	// Register ContentSources
	String sources = config.getInitParameter(CONTENT_SOURCES);
	if ((sources == null) || (sources.trim().length() == 0)) {
	    throw new ServletException("No ContentSources specified!  Ensure "
		    + "at least 1 DownloadServlet.ContentSource is provided as"
		    + " a Servlet init parameter (key: " + CONTENT_SOURCES
		    + ").");
	}
	StringTokenizer tokens = new StringTokenizer(sources, " \t\n\r\f,;:");
	while (tokens.hasMoreTokens()) {
	    registerContentSource(tokens.nextToken());
	}
    }

    /**
     *	<p> This method registers the given class name as a
     *	    {@link DownloadServlet#ContentSource}.  This method will attempt
     *	    to resolve and instantiate the class using the current
     *	    classloader.</p>
     */
    public void registerContentSource(String className) {
	// Sanity Check
	if ((className == null) || className.trim().equals("")) {
	    return;
	}

	Class cls = null;
	try {
	    cls = Class.forName(className);
	} catch (Exception ex) {
	    throw new RuntimeException(ex);
	}
	registerContentSource(cls);
    }

    /**
     *	<p> This method registers the given class name as a
     *	    {@link DownloadServlet#ContentSource}.  This method will attempt
     *	    to instantiate the class via the default constructor.</p>
     */
    public void registerContentSource(Class cls) {
	// Create a new instance
	DownloadServlet.ContentSource source = null;
	try {
	    source = (DownloadServlet.ContentSource) cls.newInstance();
	} catch (Exception ex) {
	    throw new RuntimeException(ex);
	}
	// Add the new instance to the registered ContentSources
	_contentSources.put(source.getId(), source);
    }

    /**
     *	<p> This method looks up a DownloadServlet.ContentSource given its id.
     *	    The {@link DownloadServlet#ContentSource} must be previously
     *	    registered.</p>
     */
    public DownloadServlet.ContentSource getContentSource(String id) {
	return _contentSources.get(id);
    }

    /**
     *	<p> This method delegates to the {@link #doPost()} method.</p>
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	doPost(request, response);
    }

    /**
     *	<p> This method is the main method for this class when used in an
     *	    <code>HttpServlet</code> environment.  It drives the process, which
     *	    includes creating a {@link DownloadServet#Context}, choosing the
     *	    appropriate {@link DownloadServlet#ContentSource}, and copying the
     *	    output of the {@link DownloadServlet#ContentSource} to the
     *	    <code>ServletResponse</code>'s <code>OutputStream</code>.</p>
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
	// Get the Download Context
	DownloadServlet.Context context = getDownloadContext(request, response);

	// Get the ContentSource
	ContentSource source = getContentSource(request);

	// Write Content
	writeContent(source, context);

	// Clean Up
	source.cleanUp(context);
    }

    /**
     *	<p> This method instantiates a {@link DownloadServlet.Context} and
     *	    initializes it with the Servlet, ServletConfig, ServletRequest,
     *	    and ServletResponse.</p>
     */
    protected DownloadServlet.Context getDownloadContext(HttpServletRequest request, HttpServletResponse response) {
	DownloadServlet.Context ctx =
	    (DownloadServlet.Context) request.getAttribute(DOWNLOAD_CONTEXT);
	if (ctx == null) {
	    ctx = new DownloadServlet.Context();

	    ctx.setServlet(this);
	    ctx.setServletConfig(getServletConfig());
	    ctx.setServletRequest(request);
	    request.setAttribute(DOWNLOAD_CONTEXT, ctx);
	}

	// This is done differently b/c the response may initially be null,
	// subsequent calls may provide this information
	ctx.setServletResponse(response);

	return ctx;
    }

    /**
     *	<p> This method locates the appropriate
     *	    {@link DownloadServlet#ContentSource} for this request.  It uses
     *	    the given <code>ServletRequest</code> to look for a
     *	    <b>ServletRequest Parameter</b> named {@link #CONTENT_SOURCE_ID}.
     *	    This value is used as the key when looking up registered
     *	    {@link DownloadServlet#ContentSource} implementations.
     */
    protected DownloadServlet.ContentSource getContentSource(ServletRequest request) {
	// Get the ContentSource id
	String id = request.getParameter(CONTENT_SOURCE_ID);
	if (id == null) {
	    id = getServletConfig().getInitParameter(CONTENT_SOURCE_ID);
	    if(id == null) {
		throw new RuntimeException("You must provide the '"
		    + CONTENT_SOURCE_ID + "' request parameter!");
	    }
	}

	// Get the ContentSource
	DownloadServlet.ContentSource src = getContentSource(id);
	if (src == null) {
	    throw new RuntimeException("The ContentSource with id '" + id
		    + "' is not registered!");
	}

	// Return the ContentSource
	return src;
    }

    /**
     *	<p> This method is responsible for setting the response header
     *	    information.</p>
     */
    protected void writeHeader(DownloadServlet.ContentSource source, DownloadServlet.Context context) {
	ServletResponse resp = context.getServletResponse();
	if (!(resp instanceof HttpServletResponse)) {
	    // This implementation is only valid for HttpServletResponse
	    return;
	}

	// Set the "Last-Modified" Header
	// First check context
	long longTime = source.getLastModified(context);
	if (longTime != -1) {
	    ((HttpServletResponse) resp).
		setDateHeader("Last-Modified", longTime);
	}

	// First check CONTENT_TYPE
	String contentType = (String) context.getAttribute(CONTENT_TYPE);
	if (contentType == null) {
	    // Not found yet, check EXTENSION
	    String ext = (String) context.getAttribute(EXTENSION);
	    if (ext != null) {
		contentType = mimeTypes.get(ext);
	    }
	    if (contentType == null) {
		// Default Content-type is: application/octet-stream
		contentType = DEFAULT_CONTENT_TYPE;
	    }
	}
	((HttpServletResponse) resp).setHeader("Content-type", contentType);
	
	// Write additional headers
	Object o = context.getAttribute(HEADERS);
	if (o instanceof Map) {
	    @SuppressWarnings("unchecked")
	    Map<String, String> headers = (Map<String, String>) o;
	    for (Map.Entry<String, String> h: headers.entrySet()) {
    	        ((HttpServletResponse) resp).setHeader(h.getKey(), h.getValue());
	    }
	}
	// TODO: log warning
	
    }

    /**
     *	<p> This method is responsible for copying the data from the given
     *	    <code>InputStream</code> to the <code>ServletResponse</code>'s
     *	    <code>OutputStream</code>.  The <code>InputStream</code> should be
     *	    the from the {@link DownloadServlet#ContentSource}.</p>
     */
    protected void writeContent(DownloadServlet.ContentSource source, DownloadServlet.Context context) {
	// Get the InputStream
	InputStream in = source.getInputStream(context);

	// Get the OutputStream
	ServletResponse resp = context.getServletResponse();
	if(in == null) {
	    //nothing to write
	    String jspPage = (String)context.getAttribute("JSP_PAGE_SERVED"); 
	    if(jspPage != null && (jspPage.equals("false")))  {
		try {
		    //Mainly to take care of javahelp2, bcz javahelp2 code needs an Exception to be thrown for FileNotFound.
		    //We may have to localize this message.
		    ((HttpServletResponse)resp).sendError(404, "File Not Found");
		} catch (IOException ex) {
		    //squelch it, just return.
		}
	    }
	    return;
	}
        InputStream stream = null;
	try {
	    javax.servlet.ServletOutputStream out = resp.getOutputStream();

	    // Get the InputStream
	    stream = new BufferedInputStream(in);

	    // Write the header
	    writeHeader(source, context);

	    // Copy the data to the ServletOutputStream
	    byte [] buf = new byte[512]; // Set our buffer at 512 bytes
	    int read = stream.read(buf, 0, 512);
	    while (read != -1) {
		// Write data from the OutputStream to the InputStream
		out.write(buf, 0, read);

		// Read more...
		read = stream.read(buf, 0, 512);
	    }
    
	} catch (IOException ex) {
	    throw new RuntimeException(ex);
	} finally {
            if (stream != null) {
                try {
                    // Close the Stream
                    stream.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
        }

    }


    //////////////////////////////////////////////////////////////////////////
    //	Inner Classes
    //////////////////////////////////////////////////////////////////////////

    /**
     *	<p> Implement this interface to provide an Object that is capable of
     *	    providing data to <code>DownloadServlet</code>.
     *	    <code>ContentSource</code> implementations must be thread safe.
     *	    The <code>DownloadServlet</code> will reuse the same instance when
     *	    2 requests are made to the same ContentSource type.  Instance
     *	    variables, therefore, should not be used; you may use the context
     *	    to store local information.</p>
     */
    public static interface ContentSource {

	/**
	 *  <p>	This method should return a unique string used to identify this
	 *	<code>ContentSource</code>.  This string must be specified in
	 *	order to select the appropriate <code>ContentSource</code> when
	 *	using the <code>DownloadServlet</code>.</p>
	 */
	public String getId();

	/**
	 *  <p>	This method is responsible for generating the content and
	 *	returning an InputStream to that content.  It is also
	 *	responsible for setting any attribute values in the
	 *	{@link DownloadServlet#Context}, such as {@link EXTENSION} or
	 *	{@link CONTENT_TYPE}.</p>
	 */
	public InputStream getInputStream(DownloadServlet.Context ctx);

	/**
	 *  <p>	This method may be used to clean up any temporary resources.
	 *	It will be invoked after the <code>InputStream</code> has
	 *	been completely read.</p>
	 */
	public void cleanUp(DownloadServlet.Context ctx);

	/**
	 *  <p>	This method is responsible for returning the last modified date
	 *	of the content, or -1 if not applicable.  This information will
	 *	be used for caching.</p>
	 */
	public long getLastModified(DownloadServlet.Context context);
    }


    /**
     *	<p> This class provides information about the request that may be
     *	    necessary for the <code>DownloadServlet.ContentSource</code> to
     *	    provide content.  The <code>DownloadServlet</code> is responsible
     *	    for supplying this object to the
     *	    <code>DownloadServlet.ContentSource</code>.</p>
     */
    public static class Context {

	/**
	 *  <p>	The default constructor.</p>
	 */
	public Context() {
	}

	/**
	 *  <p>	This method may be used to manage arbitrary information between
	 *	the <code>DownloadServlet</code> and the
	 *	<code>DownloadServlet.ContentSource</code>.  This method
	 *	retrieves an attribute.</p>
	 */
	public Object getAttribute(String name) {
	    if (name == null) {
		return null;
	    }

	    // First check the local attribute Map
	    Object value = _att.get(name);
	    if (value == null) {
		// Not found, check the Request attributes...
		value = getServletRequest().getParameter(name);
	    }

	    // Return the value (if any)
	    return value;
	}

	/**
	 *  <p>	This method may be used to manage arbitrary information between
	 *	the <code>DownloadServlet</code> and the
	 *	<code>DownloadServlet.ContentSource</code>.  This method sets
	 *	an attribute.</p>
	 */
	public void setAttribute(String name, Object value) {
	    if (name != null) {
		_att.put(name, value);
	    }
	}

	/**
	 *  <p>	This method may be used to manage arbitrary information between
	 *	the <code>DownloadServlet</code> and the
	 *	<code>DownloadServlet.ContentSource</code>.  This method
	 *	removes an attribute.</p>
	 */
	public void removeAttribute(String name) {
	    _att.remove(name);
	}

	/**
	 *  <p>	This returns the <code>Servlet</code> associated with the
	 *	request.  This may be cast to the specific <code>Servlet</code>
	 *	instance, such as <code>HttpServlet</code>.</p>
	 */
	public Servlet getServlet() {
	    return _servlet;
	}

	/**
	 *  <p>	This sets the <code>Servlet</code> associated with the
	 *	request.</p>
	 */
	protected void setServlet(Servlet servlet) {
	    _servlet = servlet;
	}

	/**
	 *  <p>	This returns the <code>ServletConfig</code>.</p>
	 */
	public ServletConfig getServletConfig() {
	    return _servletConfig;
	}

	/**
	 *  <p>	This sets the <code>ServletConfig</code>.</p>
	 */
	protected void setServletConfig(ServletConfig config) {
	    _servletConfig = config;
	}

	/**
	 *  <p>	This returns the <code>ServletRequest</code> associated with
	 *	the request.  This may be cast to the specific type, such as
	 *	<code>HttpServletRequest</code>.</p>
	 */
	public ServletRequest getServletRequest() {
	    return _request;
	}

	/**
	 *  <p>	This sets the <code>ServletRequest</code> associated with the
	 *	request.</p>
	 */
	protected void setServletRequest(ServletRequest request) {
	    _request = request;
	}

	/**
	 *  <p>	This returns the <code>ServletResponse</code> associated with
	 *	the request.  This may be cast to the specific type, such as
	 *	<code>HttpServletResponse</code>.</p>
	 */
	public ServletResponse getServletResponse() {
	    return _response;
	}

	/**
	 *  <p>	This sets the <code>ServletResponse</code> associated with the
	 *	request.</p>
	 */
	protected void setServletResponse(ServletResponse response) {
	    _response = response;
	}


	private	Servlet		_servlet	= null;
	private	ServletConfig	_servletConfig  = null;
	private	ServletRequest	_request	= null;
	private	ServletResponse	_response	= null;
	private	Map<String, Object> _att	= new HashMap<String, Object>();
    }

    /**
     *	<p> This method gets called before the doGet/doPost method.  The
     *	    requires us to create the {@link DownloadServlet#Context} here.
     *	    However, we do not have the <code>HttpServletResponse</code> yet,
     *	    so it will be null.</p>
     */
    protected long getLastModified(HttpServletRequest request) {
	// Get the DownloadServlet Context
	DownloadServlet.Context context = getDownloadContext(request, null);

	// Get the ContentSource
	ContentSource source = getContentSource(request);
	
	// Calculate the last modified date
	return source.getLastModified(context);
    }


    /**
     *	HashMap to hold mimetypes by extension.
     */
    private static Map<String, String> mimeTypes =
	    new HashMap<String, String>(120);
    static {
	mimeTypes.put("aif", "audio/x-aiff");
	mimeTypes.put("aifc", "audio/x-aiff");
	mimeTypes.put("aiff", "audio/x-aiff");
	mimeTypes.put("asc", "text/plain");
	mimeTypes.put("asf", "application/x-ms-asf");
	mimeTypes.put("asx", "application/x-ms-asf");
	mimeTypes.put("au", "audio/basic");
	mimeTypes.put("avi", "video/x-msvideo");
	mimeTypes.put("bin", "application/octet-stream");
	mimeTypes.put("bmp", "image/bmp");
	mimeTypes.put("bwf", "audio/wav");
	mimeTypes.put("bz2", "application/x-bzip2");
	mimeTypes.put("c", "text/plain");
	mimeTypes.put("cc", "text/plain");
	mimeTypes.put("cdda", "audio/x-aiff");
	mimeTypes.put("class", "application/octet-stream");
	mimeTypes.put("com", "application/octet-stream");
	mimeTypes.put("cpp", "text/plain");
	mimeTypes.put("cpr", "image/cpr");
	mimeTypes.put("css", "text/css");
	mimeTypes.put("doc", "application/msword");
	mimeTypes.put("dot", "application/msword");
	mimeTypes.put("dtd", "text/xml");
	mimeTypes.put("ear", "application/zip");
	mimeTypes.put("exe", "application/octet-stream");
	mimeTypes.put("flc", "video/flc");
	mimeTypes.put("fm", "application/x-maker");
	mimeTypes.put("frame", "application/x-maker");
	mimeTypes.put("frm", "application/x-maker");
	mimeTypes.put("h", "text/plain");
	mimeTypes.put("hh", "text/plain");
	mimeTypes.put("hpp", "text/plain");
	mimeTypes.put("hqx", "application/mac-binhex40");
	mimeTypes.put("htm", "text/html");
	mimeTypes.put("html", "text/html");
	mimeTypes.put("gif", "image/gif");
	mimeTypes.put("gz", "application/x-gunzip");
	mimeTypes.put("ico", "image/x-icon");
	mimeTypes.put("iso", "application/octet-stream");
	mimeTypes.put("jar", "application/zip");
	mimeTypes.put("java", "text/plain");
	mimeTypes.put("jnlp", "application/x-java-jnlp-file");
	mimeTypes.put("jpeg", "image/jpeg");
	mimeTypes.put("jpe", "image/jpeg");
	mimeTypes.put("jpg", "image/jpeg");
	mimeTypes.put("js", "text/x-javascript");
	mimeTypes.put("m3u", "audio/x-mpegurl");
	mimeTypes.put("maker", "application/x-maker");
	mimeTypes.put("mid", "audio/midi");
	mimeTypes.put("midi", "audio/midi");
	mimeTypes.put("mim", "application/mime");
	mimeTypes.put("mime", "application/mime");
	mimeTypes.put("mov", "video/quicktime");
	mimeTypes.put("mp2", "audio/mpeg");
	mimeTypes.put("mp3", "audio/mpeg");
	mimeTypes.put("mp4", "video/mpeg4");
	mimeTypes.put("mpa", "video/mpeg");
	mimeTypes.put("mpe", "video/mpeg");
	mimeTypes.put("mpeg", "video/mpeg");
	mimeTypes.put("mpg", "video/mpeg");
	mimeTypes.put("mpga", "audio/mpeg");
	mimeTypes.put("mpm", "video/mpeg");
	mimeTypes.put("mpv", "video/mpeg");
	mimeTypes.put("pdf", "application/pdf");
	mimeTypes.put("pic", "image/x-pict");
	mimeTypes.put("pict", "image/x-pict");
	mimeTypes.put("pct", "image/x-pict");
	mimeTypes.put("pl", "application/x-perl");
	mimeTypes.put("png", "image/png");
	mimeTypes.put("pnm", "image/x-portable-anymap");
	mimeTypes.put("pbm", "image/x-portable-bitmap");
	mimeTypes.put("ppm", "image/x-portable-pixmap");
	mimeTypes.put("ps", "application/postscript");
	mimeTypes.put("ppt", "application/vnd.ms-powerpoint");
	mimeTypes.put("qt", "video/quicktime");
	mimeTypes.put("ra", "application/vnd.rn-realaudio");
	mimeTypes.put("rar", "application/zip");
	mimeTypes.put("rf", "application/vnd.rn-realflash");
	mimeTypes.put("ra", "audio/vnd.rn-realaudio");
	mimeTypes.put("ram", "audio/x-pn-realaudio");
	mimeTypes.put("rm", "application/vnd.rn-realmedia");
	mimeTypes.put("rmm", "audio/x-pn-realaudio");
	mimeTypes.put("rsml", "application/vnd.rn-rsml");
	mimeTypes.put("rtf", "text/rtf");
	mimeTypes.put("rv", "video/vnd.rn-realvideo");
	mimeTypes.put("spl", "application/futuresplash");
	mimeTypes.put("snd", "audio/basic");
	mimeTypes.put("ssm", "application/smil");
	mimeTypes.put("swf", "application/x-shockwave-flash");
	mimeTypes.put("tar", "application/x-tar");
	mimeTypes.put("tgz", "application/x-gtar");
	mimeTypes.put("tif", "image/tiff");
	mimeTypes.put("tiff", "image/tiff");
	mimeTypes.put("txt", "text/plain");
	mimeTypes.put("ulw", "audio/basic");
	mimeTypes.put("war", "application/zip");
	mimeTypes.put("wav", "audio/x-wav");
	mimeTypes.put("wax", "application/x-ms-wax");
	mimeTypes.put("wm", "application/x-ms-wm");
	mimeTypes.put("wma", "application/x-ms-wma");
	mimeTypes.put("wml", "text/wml");
	mimeTypes.put("wmw", "application/x-ms-wmw");
	mimeTypes.put("wrd", "application/msword");
	mimeTypes.put("wvx", "application/x-ms-wvx");
	mimeTypes.put("xbm", "image/x-xbitmap");
	mimeTypes.put("xpm", "image/image/x-xpixmap");
	mimeTypes.put("xml", "text/xml");
	mimeTypes.put("xsl", "text/xml");
	mimeTypes.put("xls", "application/vnd.ms-excel");
	mimeTypes.put("zip", "application/zip");
	mimeTypes.put("z", "application/x-compress");
	mimeTypes.put("Z", "application/x-compress");
    }


    private static Map<String, DownloadServlet.ContentSource> _contentSources =
	    new HashMap<String, DownloadServlet.ContentSource>();

    /**
     *	<p> This String ("downloadContext") is the name if the
     *	    <b>ServletRequest Attribute</b> used to store the
     *	    {@link DownloadServlet#Context} object for this request.</p>
     */
    public static final String DOWNLOAD_CONTEXT	    = "downloadContext";

    /**
     *	<p> This String ("ContentSources") is the name if the <b>Servlet Init
     *	    Parameter</b> that should be used to register all available
     *	    {@link DownloadServlet#ContentSource} implementations.</p>
     */
    public static final String CONTENT_SOURCES	    = "ContentSources";

    /**
     *	<p> This is the <b>ServletRequest Parameter</b> that should be provided
     *	    to identify the <code>DownloadServlet.ContentSource</code>
     *	    implementation that should be used.  This value must match the
     *	    value returned by the <code>DownloadServlet.ContentSource</code>
     *	    implementation's <code>getId()</code> method.</p>
     */
    public static final String CONTENT_SOURCE_ID    = "contentSourceId";

    /**
     *	<p> The Content-type ("ContentType").  This is the
     *	    {@link DownloadServlet#Context} attribute used to specify an
     *	    explicit "Content-type".  It may be set by the
     *	    {@link DownloadServlet#ContentSource}, or may be passed in via a
     *	    request parameter.  If not specified, the {@link #EXTENSION} will
     *	    be used.  If that fails, the {@link #DEFAULT_CONTENT_TYPE} will
     *	    apply.</p>
     */
    public static final String CONTENT_TYPE = "ContentType";

    /**
     *	<p> The Default Content-type ("application/octet-stream").</p>
     */
    public static final String DEFAULT_CONTENT_TYPE =
	    "application/octet-stream";

    /**
     *	<p> This is the {@link DownloadServlet#Context} attribute name used to
     *	    specify the filename extension of the content.  It is the
     *	    responsibility of the {@link DownloadServlet#ContentSource} to set
     *	    this value.  The value should represent the filename extension of
     *	    the content if it were saved to a filesystem.</p>
     */
    public static final String EXTENSION = "extension";

    /**
     *  <p> This is the {@link DownloadServlet#Context} attribute name used to
     *      specify optional additional headers.  It must be set to
     *      <codde>Map<String, String></code> object when needed.</p>
     */
    public static final String HEADERS = "Headers";
}

