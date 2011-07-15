/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * Support for reading ZIP/JAR files.
 */
#include "zip_util.h"
#include "java.h"

#define MAXREFS 0xFFFF	/* max number of open zip file references */
#define MAXSIZE INT_MAX	/* max size of zip file or zip entry */

static jzfile *zfiles = 0;	/* currently open zip files */
static char errbuf[256];

/*
 * Initialize zip file support. Return 0 if successful otherwise -1
 * if could not be initialized.
 */
static jint initializeZip()
{
    extern void out_of_memory(void);
    static jboolean inited = JNI_FALSE;
    if (inited)
        return 0;
    inited = JNI_TRUE;

    return 0;
}

/*
 * Reads len bytes of data into buf. Returns 0 if all bytes could be read,
 * otherwise returns -1.
 */
static jint readFully(int fd, void *buf, jint len)
{
    unsigned char *bp = buf;
    while (len > 0)
    {
        jint n = read(fd, (char *)bp, len);
        if (n <= 0)
            return -1;
        bp += n;
        len -= n;
    }
    return 0;
}

/*
 * Allocates a new zip file object for the specified file name.
 * Returns the zip file object or NULL if not enough memory.
 */
static jzfile *allocZip(const char *name)
{
    jzfile *zip = calloc(1, sizeof(jzfile));

    if (zip == 0)
        return 0;
    zip->name = strdup(name);
    if (zip->name == 0)
    {
        free(zip);
        return 0;
    }
    return zip;
}

/*
 * Frees the specified zip file object.
 */
static void freeZip(jzfile *zip)
{
    int i;
    /* First free any cached jzentry */
    zipFreeEntry(zip,0);
    if (zip->name != 0)
        free(zip->name);
    if (zip->comment != 0)
        free(zip->comment);
    if (zip->entries != 0)
        free(zip->entries);
    if (zip->table != 0)
        free(zip->table);
    if (zip->metanames != 0)
    {
        for (i = 0; i < zip->metacount; i++)
        {
            if (zip->metanames[i])
                free(zip->metanames[i]);
        }
        free(zip->metanames);
    }
    if (zip->comments != 0)
    {
        for (i = 0; i < zip->total; i++)
        {
            if (zip->comments[i])
                free(zip->comments[i]);
        }
        free(zip->comments);
    }
    free(zip);
}

/*
 * Searches for end of central directory (END) header. The contents of
 * the END header will be read and placed in endbuf. Returns the file
 * position of the END header, otherwise returns 0 if the END header
 * was not found or -1 if an error occurred.
 */
static jint findEND(jzfile *zip, void *endbuf)
{
    unsigned char buf[ENDHDR * 2];
    jint len, pos;
    int fd = zip->fd;

    /* Get the length of the zip file */
    len = pos = lseek(fd, 0, SEEK_END);
    if (len == -1)
        return -1;

    /*
     * Search backwards ENDHDR bytes at a time from end of file stopping
     * when the END header has been found. We need to make sure that we
     * handle the case where the signature may straddle a record boundary.
     * Also, the END header must be located within the last 64k bytes of
     * the file since that is the maximum comment length.
     */
    memset(buf, 0, sizeof(buf));
    while (len - pos < 0xFFFF)
    {
        unsigned char *bp;
        /* Number of bytes to check in next block */
        int count = 0xFFFF - (len - pos);
        if (count > ENDHDR)
            count = ENDHDR;
        /* Shift previous block */
        memcpy(buf + count, buf, count);
        /* Update position and read next block */
        pos -= count;
        if (lseek(fd, pos, SEEK_SET) == -1)
            return -1;
        if (readFully(fd, buf, count) == -1)
            return -1;
        /* Now scan the block for END header signature */
        for (bp = buf; bp < buf + count; bp++)
        {
            if (GETSIG(bp) == ENDSIG)
            {
        	/* Check for possible END header */
        	jint endpos = pos + (bp - buf);
        	jint clen = ENDCOM(bp);
        	if (endpos + ENDHDR + clen == len)
                {
        	    /* Found END header */
        	    memcpy(endbuf, bp, ENDHDR);
        	    if (lseek(fd, endpos + ENDHDR, SEEK_SET) == -1)
        		return -1;
        	    if (clen > 0)
                    {
        		zip->comment = malloc(clen + 1);
        		if (zip->comment == 0)
        		    return -1;
        		if (readFully(zip->fd, zip->comment, clen) == -1)
                        {
        		    free(zip->comment);
        		    zip->comment = 0;
        		    return -1;
        		}
        		zip->comment[clen] = '\0';
        	    }
        	    return endpos;
        	}
            }
        }
    }
    return 0; /* END header not found */
}

/*
 * Returns a hash code value for the specified string.
 */
static unsigned int
hash(const char *s)
{
    int h = 0;
    while (*s != '\0')
        h = 31*h + *s++;
    return h;
}

/*
 * Returns true if the specified entry's name begins with the string
 * "META-INF/" irrespect of case.
 */
static int
isMetaName(char *name)
{
#define META_INF "META-INF/"
    char *s = META_INF, *t = name;
    while (*s != '\0')
    {
        if (*s++ != (char)toupper(*t++))
            return 0;
    }
    return 1;
}

static void
addMetaName(jzfile *zip, char *name)
{
    int i;
    if (zip->metanames == 0)
    {
        zip->metacount = 2;
        zip->metanames = calloc(zip->metacount, sizeof(char *));
    }
    for (i = 0; i < zip->metacount; i++)
        {
        if (zip->metanames[i] == 0)
        {
            zip->metanames[i] = strdup(name);
            break;
        }
    }
    /* If necessary, grow the metanames array */
    if (i >= zip->metacount)
    {
        int new_count = 2 * zip->metacount;
        char **tmp = calloc(new_count, sizeof(char *));
        for (i = 0; i < zip->metacount; i++)
            tmp[i] = zip->metanames[i];
        tmp[i] = strdup(name);
        free(zip->metanames);
        zip->metanames = tmp;
        zip->metacount = new_count;
    }
}

static void
addEntryComment(jzfile *zip, int index, char *comment)
{
    if (zip->comments == NULL)
        zip->comments = calloc(zip->total, sizeof(char *));
    zip->comments[index] = comment;
}

/*
 * Reads zip file central directory. Returns the file position of first
 * CEN header, otherwise returns 0 if central directory not found or -1
 * if an error occurred. If zip->msg != NULL then the error was a zip
 * format error and zip->msg has the error text.
 */
static
jint readCEN(jzfile *zip)
{
    jint endpos, locpos, cenpos, cenoff, cenlen;
    jint total, count, tablelen, i, tmplen;
    unsigned char endbuf[ENDHDR], *cenbuf, *cp;
    jzcell *entries;
    unsigned short *table;
    char namebuf[ZIP_TYPNAMELEN + 1];
    char* name = namebuf;
    int namelen = ZIP_TYPNAMELEN + 1;


    /* Clear previous zip error */
    zip->msg = 0;
    /* Get position of END header */
    endpos = findEND(zip, endbuf);
    if (endpos == 0)
        return 0;  /* END header not found */
    if (endpos == -1)
        return -1; /* system error */
    /* Get position and length of central directory */
    cenlen = ENDSIZ(endbuf);
    if (cenlen < 0 || cenlen > endpos)
    {
        zip->msg = "invalid END header (bad central directory size)";
        return -1;
    }
    cenpos = endpos - cenlen;
    /*
     * Get position of first local file (LOC) header, taking into
     * account that there maybe a stub prefixed to the zip file.
     */ 
    cenoff = ENDOFF(endbuf);
    if (cenoff < 0 || cenoff > cenpos)
    {
        zip->msg = "invalid END header (bad central directory offset)";
        return -1;
    }
    locpos = cenpos - cenoff;
    /* Get total number of central directory entries */
    total = zip->total = ENDTOT(endbuf);
    if (total < 0 || total * CENHDR > cenlen)
    {
        zip->msg = "invalid END header (bad entry count)";
        return -1;
    }
    if (total > ZIP_MAXENTRIES)
    {
        zip->msg = "too many entries in ZIP file";
        return -1;
    }
    /* Seek to first CEN header */
    if (lseek(zip->fd, cenpos, SEEK_SET) == -1)
        return -1;

    /* Allocate temporary buffer for central directory bytes */
    cenbuf = malloc(cenlen);
    if (cenbuf == 0)
        return -1;
    /* Read central directory */
    if (readFully(zip->fd, cenbuf, cenlen) == -1)
    {
        free(cenbuf);
        return -1;
    }
    /* Allocate array for item descriptors */
    entries = zip->entries = calloc(total, sizeof(jzcell));
    if (entries == 0)
    {
        free(cenbuf);
        return -1;
    }
    /* Allocate hash table */
    tmplen = total/2;
    tablelen = zip->tablelen = (tmplen > 0 ? tmplen : 1);
    table = zip->table = calloc(tablelen, sizeof(unsigned short));
    if (table == 0)
    {
        free(cenbuf);
        free(entries);
        zip->entries = 0;
        return -1;
    }
    for (i = 0; i < tablelen; i++)
        table[i] = ZIP_ENDCHAIN;

    /* Now read the zip file entries */
    for (count = 0, cp = cenbuf; count < total; count++)
    {
        jzcell *zc = &entries[count];
        int method, nlen, clen, elen, hsh;

        /* Check CEN header looks OK */
        if ((cp - cenbuf) + CENHDR > cenlen)
        {
            zip->msg = "invalid CEN header (bad header size)";
            break;
        }
        /* Verify CEN header signature */
        if (GETSIG(cp) != CENSIG)
        {
            zip->msg = "invalid CEN header (bad signature)";
            break;
        }
        /* Check if entry is encrypted */
        if ((CENVER(cp) & 1) == 1)
        {
            zip->msg = "invalid CEN header (encrypted entry)";
            break;
        }
        method = CENHOW(cp);
        if (method != STORED && method != DEFLATED)
        {
            zip->msg = "invalid CEN header (bad compression method)";
            break;
        }

        /* Get header field lengths */
        nlen         = CENNAM(cp);
        elen         = CENEXT(cp);
        clen         = CENCOM(cp);
        if ((cp - cenbuf) + CENHDR + nlen + clen + elen > cenlen)
        {
            zip->msg = "invalid CEN header (bad header size)";
            break;
        }

        zc->size     = CENLEN(cp);
        zc->csize    = CENSIZ(cp);
        zc->crc      = CENCRC(cp);
        /* Set compressed size to zero if entry uncompressed */
        if (method == STORED)
            zc->csize = 0;

        /*
         * Copy the name into a temporary location so we can null
         * terminate it (sigh) as various functions expect this.
         */
        if (namelen < nlen + 1)
        {
            /* grow temp buffer */
            do  
                namelen = namelen * 2;
            while (namelen < nlen + 1);
            if (name != namebuf) /* free malloc()ated buffer */
                free(name);
            name = malloc(namelen);
        } 
        memcpy(name, cp+CENHDR, nlen);
        name[nlen] = 0;

        /*
         * Record the LOC offset and the name hash in our hash cell.
         */
        zc->pos = CENOFF(cp) + locpos;
        zc->nelen = nlen + elen;
        zc->hash = hash(name);
     	zc->cenpos = cenpos + (cp - cenbuf);
        zc->elen = elen;
        /*
         * if the entry is metdata add it to our metadata names
         */
        if (isMetaName(name))
            addMetaName(zip, name);

        /*
         * If there is a comment add it to our comments array.
         */
        if (clen > 0)
        {
            char *comment = malloc(clen+1);
            memcpy(comment, cp+CENHDR+nlen+elen, clen);
            comment[clen] = 0;
            addEntryComment(zip, count, comment);
 	}

        /*
         * Finally we can add the entry to the hash table
         */
        hsh = zc->hash % tablelen;
        zc->next = table[hsh];
        table[hsh] = count;

        cp += (CENHDR + nlen + elen + clen);
    }
    /* Free up temporary buffers */
    free(cenbuf);
    if (name != namebuf)
        free(name);

    /* Check for error */
    if (count != total)
    {
        /* Central directory was invalid, so free up entries and return */
        free(entries);
        zip->entries = 0;
        free(table);
        zip->table = 0;
        return -1;
    }
    return cenpos;
}

/*
 * Opens a zip file with the specified mode. Returns the jzfile object 
 * or NULL if an error occurred. If a zip error occurred then *msg will 
 * or NULL if an error occurred. If a zip error occurred then *zerror will be
 * set to the error number. Otherwise, *zerror will be set to Z_OK.
 */
jzfile *
zipOpenGeneric(const char *name, int *zerror, int mode, jlong lastModified)
{
    jzfile *zip;

    if (initializeZip())
        return NULL;

    /* Clear zip error message */
    if (zerror != 0)
        *zerror = Z_OK;

    for (zip = zfiles; zip != 0; zip = zip->next)
    {
        if (strcmp(name, zip->name) == 0
            && (zip->lastModified == lastModified || zip->lastModified == 0)
            && zip->refs < MAXREFS)
        {
            zip->refs++;
            break;
        }
    }
    if (zip == 0)
    {
        jlong len;
        /* If not found then allocate a new zip object */
        zip = allocZip(name);
        if (zip == 0)
            return 0;
        zip->refs = 1;
        zip->lastModified = lastModified;
        zip->fd = open(name, mode);
        if (!zip->fd)
        {
            if (zerror != 0)
                *zerror = Z_MEM_ERROR;
            freeZip(zip);
            return 0;
        }
        len = lseek(zip->fd, 0, SEEK_END);
        if (len == -1)
        {
            if (zerror != 0)
                *zerror = Z_STREAM_ERROR;
            close(zip->fd);
            freeZip(zip);
            return 0;
        }
        if (len > MAXSIZE)
        {
            if (zerror != 0)
                *zerror = Z_STREAM_ERROR;
            close(zip->fd);
            freeZip(zip);
            return 0;
        }
        if (readCEN(zip) <= 0)
        {
            /* An error occurred while trying to read the zip file */
            if (zerror != 0)
                *zerror = Z_STREAM_ERROR;
            close(zip->fd);
            freeZip(zip);
            return 0;
        }
        zip->next = zfiles;
        zfiles = zip;
    }
    return zip;
}

/*
 * Opens a zip file for reading. Returns the jzfile object or NULL
 * if an error occurred. If a zip error occurred then *zerror will be
 * set to the error number. Otherwise, *zerror will be set to Z_OK.
 */
jzfile *
zipOpen(const char *name, int *zerror)
{
#ifdef WIN32
    int mode = O_RDONLY | O_BINARY;
#else
    int mode = O_RDONLY;
#endif
    return zipOpenGeneric(name, zerror, mode, 0);
}

/*
 * Closes the specified zip file object.
 */
void zipClose(jzfile *zip)
{
    if (--zip->refs > 0)
    {
        /* Still more references so just return */
        return;
    }
    /* No other references so close the file and remove from list */
    if (zfiles == zip)
    {
        zfiles = zfiles->next;
    }
    else
    {
        jzfile *zp;
        for (zp = zfiles; zp->next != 0; zp = zp->next)
        {
            if (zp->next == zip)
            {
        	zp->next = zip->next;
        	break;
            }
        }
    }
    close(zip->fd);
    freeZip(zip);
    return;
}

/*
 * Read a LOC corresponding to a given hash cell and
 * create a corrresponding jzentry entry descriptor
 * The ZIP lock should be held here.
 */
static jzentry *
readLOC(jzfile *zip, jzcell *zc)
{
    unsigned char *locbuf;
    jint nelen = zc->nelen;
    jint nlen, elen;
    jzentry *ze;

    /* Seek to beginning of LOC header */
    if (lseek(zip->fd, zc->pos, SEEK_SET) == -1)
    {
        zip->msg = "seek failed";
        return NULL;
    }   

    locbuf =  malloc(LOCHDR + nelen);
    /* Try to read in the LOC header including the name and extra data */
    if (readFully(zip->fd, locbuf, LOCHDR+nelen) == -1)
    {
        zip->msg = "couldn't read LOC header";
        free(locbuf);
        return NULL;
    }

    /* Verify signature */
    if (GETSIG(locbuf) != LOCSIG)
    {
        zip->msg = "invalid LOC header (bad signature)";
        free(locbuf);
        return NULL;
    }

    /* verify lengths */
    nlen = LOCNAM(locbuf);
    elen = LOCEXT(locbuf);

    ze = calloc(1, sizeof(jzentry));
    ze->name = malloc(nlen + 1);
    memcpy(ze->name, locbuf+LOCHDR, nlen);
    ze->name[nlen] = 0;

    /* If extra in CEN, use it instead of extra in LOC */
    if (zc->elen > 0)
    {
        int off = CENHDR + zc->nelen - zc->elen + zc->cenpos;
        elen = zc->elen;
        ze->extra = malloc(elen+2);
        ze->extra[0] = (unsigned char)elen;
        ze->extra[1] = (unsigned char)(elen >> 8);

        /* Seek to begin of CEN header extra field */
        if (lseek(zip->fd, off, SEEK_SET) == -1)
        {
            zip->msg = "seek failed";
            free(locbuf);
            return NULL;
        }
        /* Try to read in the CEN Extra */
        if (readFully(zip->fd, &ze->extra[2], elen) == -1)
        {
            zip->msg = "couldn't read CEN extra";
            free(locbuf);
            return NULL;
        }
    }
    else if (LOCEXT(locbuf) != 0)
    {
        ze->extra = malloc(elen + 2);
        /* Store the extra data size in the first two bytes */
        ze->extra[0] = (unsigned char)elen;
        ze->extra[1] = (unsigned char)(elen >> 8);
        memcpy(&ze->extra[2], locbuf+LOCHDR+nlen, elen);
    }

    /*
     * Process any comment (this should be very rare)
     */
    if (zip->comments)
    {	
        int index = zc - zip->entries;
        ze->comment = zip->comments[index];
    }

    /*
     * We'd like to initialize the sizes from the LOC, but unfortunately
     * some ZIPs, including the jar command, don't put them there.
     * So we have to store them in the szcell.
     */
    ze->size = zc->size;
    ze->csize = zc->csize;
    ze->crc = zc->crc;

    /* Fill in the rest of the entry fields from the LOC */
    ze->time = LOCTIM(locbuf);
    ze->pos = zc->pos + LOCHDR + LOCNAM(locbuf) + LOCEXT(locbuf);
    free(locbuf);

    return ze;
}

/*
 * Free the given jzentry.
 * In fact we maintain a one-entry cache of the most recently used
 * jzentry for each zip.  This optimizes a common access pattern.
 */

void
zipFreeEntry(jzfile *jz, jzentry *ze)
{
    jzentry *last;
    last = jz->cache;
    jz->cache = ze;
    if (last != NULL)
    {
        /* Free the previously cached jzentry */
        if (last->extra)
            free(last->extra);
        if (last->name)
            free(last->name);
        free(last);
    }
}

/*
 * Returns the zip entry corresponding to the specified name, or
 * NULL if not found.
 */
jzentry * zipGetEntry(jzfile *zip, const char *name)
{
    unsigned int hsh = hash(name);
    int idx = zip->table[hsh % zip->tablelen];
    jzentry *ze;

    /* Check the cached entry first */
    ze = zip->cache;
    if (ze && strcmp(ze->name,name) == 0)
    {
        /* Cache hit!  Remove and return the cached entry. */
        zip->cache = 0;
        return ze;
    }
    ze = 0;

    /*
     * Search down the target hash chain for a cell who's
     * 32 bit hash matches the hashed name.
     */
    while (idx != ZIP_ENDCHAIN)
    {
        jzcell *zc = &zip->entries[idx];

        if (zc->hash == hsh)
        {
            /*
             * OK, we've found a ZIP entry whose 32 bit hashcode
             * matches the name we're looking for.  Try to read its
             * entry information from the LOC.
             * If the LOC name matches the name we're looking,
             * we're done.  
             * If the names don't (which should be very rare) we
             * keep searching.
             */
            ze = readLOC(zip, zc);
            if (ze && strcmp(ze->name, name)==0)
        	break;
            if (ze != 0)
        	zipFreeEntry(zip, ze);
            ze = 0;
        }
        idx = zc->next;
    }
    return ze;
}

/*
 * Returns the n'th (starting at zero) zip file entry, or NULL if the
 * specified index was out of range.
 */
jzentry *
zipGetNextEntry(jzfile *zip, jint n)
{
    jzentry *result;
    if (n < 0 || n >= zip->total)
        return 0;
    result = readLOC(zip, &zip->entries[n]);
    return result;
}

/*
 * Reads bytes from the specified zip entry. Returns the
 * number of bytes read, or -1 if an error occurred. If err->msg != 0
 * then a zip error occurred and err->msg contains the error text.
 */
jint zipRead(jzfile *zip, jzentry *entry, jint pos, void *buf, jint len)
{
    jint n, avail, size;

    /* Clear previous zip error */
    zip->msg = 0;
    /* Check specified position */
    size = entry->csize != 0 ? entry->csize : entry->size;
    if (pos < 0 || pos > size - 1)
    {
        zip->msg = "zipRead: specified offset out of range";
        return -1;
    }
    /* Check specified length */
    if (len <= 0)
        return 0;
    avail = size - pos;
    if (len > avail)
        len = avail;

    /* Seek to beginning of entry data and read bytes */
    n = lseek(zip->fd, entry->pos + pos, SEEK_SET);
    if (n != -1)
        n = read(zip->fd, buf, len);
    return n;
}

/* The maximum size of a stack-allocated buffer.
 */
#define BUF_SIZE 4096

/*
 * This function is used by the runtime system to load compressed entries
 * from ZIP/JAR files specified in the class path. It is defined here
 * so that it can be dynamically loaded by the runtime if the zip library
 * is found.
 */
static jboolean
inflateFully(jzfile *zip, jzentry *entry, int fd, char **msg)
{
    z_stream strm;
    char tmp[BUF_SIZE];
    unsigned char buf[BUF_SIZE * 4];
    jint pos = 0, count = entry->csize;

    *msg = 0; /* Reset error message */

    if (count == 0)
    {
        *msg = "inflateFully: entry not compressed";
        return JNI_FALSE;
    }

    memset(&strm, 0, sizeof(z_stream));
    if (inflateInit2(&strm, -MAX_WBITS) != Z_OK)
    {
        *msg = strm.msg;
        return JNI_FALSE;
    }

    while (count > 0)
    {
        jint n = count > (jint)sizeof(tmp) ? (jint)sizeof(tmp) : count;
        n = zipRead(zip, entry, pos, tmp, n);
        if (n <= 0)
        {
            if (n == 0)
                *msg = "inflateFully: Unexpected end of file";
            inflateEnd(&strm);
            return JNI_FALSE;
        }
        pos += n;
        count -= n;
        strm.next_in = (Bytef *)tmp;
        strm.avail_in = n;

        do
        {
            strm.next_out = buf;
            strm.avail_out = sizeof(buf);
            if (inflate(&strm, Z_PARTIAL_FLUSH) == Z_STREAM_END)
            {
        	if (count != 0 || strm.total_out != entry->size)
                {
        	    *msg = "inflateFully: Unexpected end of stream";
                    inflateEnd(&strm);
        	    return JNI_FALSE;
        	}
            }
            if (write(fd, buf, sizeof(buf) - strm.avail_out) != sizeof(buf) - strm.avail_out)
            {
                inflateEnd(&strm);
        	return JNI_FALSE;
            }
        } while (strm.avail_in > 0 || !strm.avail_out);
    }
    inflateEnd(&strm);
    return JNI_TRUE;
}

jzentry *
zipFindEntry(jzfile *zip, const char *name, jint *sizeP, jint *nameLenP)
{
    jzentry *entry = zipGetEntry(zip, name);
    if (entry)
    {
        *sizeP = entry->size;
        *nameLenP = strlen(entry->name);
    }
    return entry;
}

/*
 * Reads a zip file entry into the specified byte array into the specified
 * file.
 */
jboolean
zipReadEntry(jzfile *zip, jzentry *entry, const char *path)
{
    char *msg;
    int fd;

#ifdef WIN32
    if ((fd = open(path, O_WRONLY | O_CREAT | O_BINARY, _S_IREAD | _S_IWRITE)) < 0)
#else
    if ((fd = open(path, O_WRONLY | O_CREAT, S_IRWXU)) < 0)
#endif
    {
        msg = "zipReadEntry: cannot open output file";
        return JNI_FALSE;
    }
    if (entry->csize == 0)
    {
        /* Entry is stored */
        jint pos = 0, count = entry->size;
        unsigned char buf[BUF_SIZE * 4];
        while (count > 0)
        {
            jint n;
            n = zipRead(zip, entry, pos, buf, count > sizeof(buf) ? sizeof(buf) : count);
            msg = zip->msg;
            if (n == -1 || write(fd, buf, n) != n)
            {
                close(fd);
                return JNI_FALSE;
            }
            pos += n;
            count -= n;
        }
    }
    else
    {
        /* Entry is compressed */
        int ok = inflateFully(zip, entry, fd, &msg);
        if (!ok)
        {
            if ((msg == NULL) || (*msg == 0))
                msg = zip->msg;
            close(fd);
            return JNI_FALSE;
        }
    }
  
    close(fd);
  
    return JNI_TRUE;
}

