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
 * Prototypes for zip file support
 */

#ifndef _ZIP_H_
#define _ZIP_H_

#include <jni.h>
#include "zlib.h"

/*
 * Header signatures
 */
#define LOCSIG 0x04034b50L	    /* "PK\003\004" */
#define EXTSIG 0x08074b50L	    /* "PK\007\008" */
#define CENSIG 0x02014b50L	    /* "PK\001\002" */
#define ENDSIG 0x06054b50L	    /* "PK\005\006" */

/*
 * Header sizes including signatures
 */
#define LOCHDR 30
#define EXTHDR 16
#define CENHDR 46
#define ENDHDR 22

/*
 * Header field access macros
 */
#define CH(b, n) (((unsigned char *)(b))[n])
#define SH(b, n) (CH(b, n) | (CH(b, n+1) << 8))
#define LG(b, n) (SH(b, n) | (SH(b, n+2) << 16))
#define GETSIG(b) LG(b, 0)

/*
 * Macros for getting local file (LOC) header fields
 */
#define LOCVER(b) SH(b, 4)	    /* version needed to extract */
#define LOCFLG(b) SH(b, 6)	    /* general purpose bit flags */
#define LOCHOW(b) SH(b, 8)	    /* compression method */
#define LOCTIM(b) LG(b, 10)	    /* modification time */
#define LOCCRC(b) LG(b, 14)	    /* crc of uncompressed data */
#define LOCSIZ(b) LG(b, 18)	    /* compressed data size */
#define LOCLEN(b) LG(b, 22)	    /* uncompressed data size */
#define LOCNAM(b) SH(b, 26)	    /* filename length */
#define LOCEXT(b) SH(b, 28)	    /* extra field length */

/*
 * Macros for getting extra local (EXT) header fields
 */
#define EXTCRC(b) LG(b, 4)	    /* crc of uncompressed data */
#define EXTSIZ(b) LG(b, 8)	    /* compressed size */
#define EXTLEN(b) LG(b, 12)         /* uncompressed size */

/*
 * Macros for getting central directory header (CEN) fields
 */
#define CENVEM(b) SH(b, 4)	    /* version made by */
#define CENVER(b) SH(b, 6)	    /* version needed to extract */
#define CENFLG(b) SH(b, 8)	    /* general purpose bit flags */
#define CENHOW(b) SH(b, 10)	    /* compression method */
#define CENTIM(b) LG(b, 12)	    /* modification time */
#define CENCRC(b) LG(b, 16)	    /* crc of uncompressed data */
#define CENSIZ(b) LG(b, 20)	    /* compressed size */
#define CENLEN(b) LG(b, 24)	    /* uncompressed size */
#define CENNAM(b) SH(b, 28)	    /* length of filename */
#define CENEXT(b) SH(b, 30)	    /* length of extra field */
#define CENCOM(b) SH(b, 32)	    /* file comment length */
#define CENDSK(b) SH(b, 34)	    /* disk number start */
#define CENATT(b) SH(b, 36)	    /* internal file attributes */
#define CENATX(b) LG(b, 38)	    /* external file attributes */
#define CENOFF(b) LG(b, 42)	    /* offset of local header */

/*
 * Macros for getting end of central directory header (END) fields
 */
#define ENDSUB(b) SH(b, 8)	    /* number of entries on this disk */
#define ENDTOT(b) SH(b, 10)	    /* total number of entries */
#define ENDSIZ(b) LG(b, 12)	    /* central directory size */
#define ENDOFF(b) LG(b, 16)	    /* central directory offset */
#define ENDCOM(b) SH(b, 20)	    /* size of zip file comment */

/*
 * Supported compression methods
 */
#define STORED	    0
#define DEFLATED    8

/*
 * Support for reading ZIP/JAR files. Some things worth noting:
 *
 * - Zip files larger than MAX_INT bytes are not supported.
 * - Zip file entries larger than INT_MAX bytes are not supported.
 * - Maximum number of entries is INT_MAX.
 * - jzentry time and crc fields are signed even though they really
 *   represent unsigned quantities.
 * - If csize is zero then entry is uncompressed.
 * - If extra != 0 then the first two bytes are the length of the extra
 *   data in intel byte order.
 * - If pos is negative then is position of entry LOC header. It is set
 *   to position of entry data once it is first read.
 */

typedef struct jzentry {  /* Zip file entry */
    char *name;	  	  /* entry name */
    jint time;            /* modification time */
    jint size;	  	  /* size of uncompressed data */
    jint csize;  	  /* size of compressed data (zero if uncompressed) */
    jint crc;		  /* crc of uncompressed data */
    char *comment;	  /* optional zip file comment */
    jbyte *extra;	  /* optional extra data */
    jint pos;	  	  /* position of LOC header (if negative) or data */
} jzentry;

/*
 * In-memory hash table cell.
 * In a typical sytem we have a *lot* of these, as we have one for
 * every entry in every activee JAR.
 * Note that in order to save space we don't keep the name in memory,
 * but merely remember a 32 bit hash.
 */
typedef struct jzcell {
    jint pos;                 	/* Offset of LOC within ZIP file */
    unsigned int hash;		/* 32 bit hashcode on name */
    unsigned short nelen;       /* length of name and extra data */
    unsigned short next;      	/* hash chain: index into jzfile->entries */
    jint size;			/* Uncompressed size */
    jint csize;			/* Compressed size */
    jint crc;
    unsigned short elen;        /* length of extra data in CEN */
    jint cenpos;                /* Offset of file headers in CEN */
} jzcell;

/*
 * Descriptor for a ZIP file.
 */
typedef struct jzfile {   /* Zip file */
    char *name;	  	  /* zip file name */
    jint refs;		  /* number of active references */
    int fd;		  /* open file descriptor */
    void *lock;		  /* read lock */
    char *comment; 	  /* zip file comment */
    char *msg;		  /* zip error message */
    jzcell *entries;      /* array of hash cells */
    jint total;	  	  /* total number of entries */
    unsigned short *table;    /* Hash chain heads: indexes into entries */
    jint tablelen;	  /* number of hash eads */
    struct jzfile *next;  /* next zip file in search list */
    jzentry *cache;       /* we cache the most recently freed jzentry */
    /* Information on metadata names in META-INF directory */
    char **metanames;     /* array of meta names (may have null names) */
    jint metacount;	  /* number of slots in metanames array */
    /* If there are any per-entry comments, they are in the comments array */
    char **comments;
    jlong lastModified;   /* last modified time */
} jzfile;

/*
 * We impose  arbitrary but reasonable limit on ZIP files.
 */
#define ZIP_MAXENTRIES (0x10000 - 2)

/* 
 * Typical size of entry name 
 */
#define ZIP_TYPNAMELEN 512 


/*
 * Index representing end of hash chain
 */
#define ZIP_ENDCHAIN 0xFFFF

jzentry *
zipFindEntry(jzfile *zip, const char *name, jint *sizeP, jint *nameLenP);

jboolean
zipReadEntry(jzfile *zip, jzentry *entry, const char *path);

jzentry *
zipGetNextEntry(jzfile *zip, jint n);

jzfile *
zipOpen(const char *name, int *zerror);

jzentry * zipGetEntry(jzfile *zip, const char *name);
void zipClose(jzfile *zip);
jint zipRead(jzfile *zip, jzentry *entry, jint pos, void *buf, jint len);
void zipFreeEntry(jzfile *zip, jzentry *ze);

#endif /* !_ZIP_H_ */ 

