# disksearch

Indexes a directory hierarchy and provides a crude search interface onto that index

Uses Lucene for managing the inverted index and Tika, Pdfbox and POI for parsing content (to be indexed).

Example of indexing:
```
➜ java -jar target/disksearch-1.1-SNAPSHOT.jar index /Users/froran/Documents
       1 Screenshot 2024-02-24 at 16.12.27.png
       2 planverk.rtf
       3 Screenshot 2021-11-06 at 16.07.42.png
       ...
       
------------------------------------------------------------------------------------
  Processed 606539 file(s)
------------------------------------------------------------------------------------


------------------------------------------------------------------------------------
                          All observed content types
------------------------------------------------------------------------------------
   application/atom+xml
   application/dicom
   application/dita+xml; format=concept
   application/gzip
   application/java-vm
   application/javascript
   application/mac-binhex40
   application/mathematica
   application/mbox
   application/msword
   application/octet-stream
   application/ogg
   application/pdf
   application/pkcs7-signature
   application/postscript
   application/rdf+xml
   application/rss+xml
   application/rtf
   application/smil+xml
   application/vnd.adobe.xdp+xml
   application/vnd.iccprofile
   application/vnd.mif
   application/vnd.ms-cab-compressed
   application/vnd.ms-excel
   application/vnd.ms-fontobject
   application/vnd.ms-htmlhelp
   application/vnd.oasis.opendocument.graphics
   application/vnd.oasis.opendocument.presentation
   application/vnd.oasis.opendocument.text
   application/vnd.sun.xml.writer
   application/vnd.tcpdump.pcap
   application/vnd.tcpdump.pcapng
   application/vnd.xara
   application/warc
   application/wasm
   application/x-7z-compressed
   application/x-archive
   application/x-bat
   application/x-berkeley-db; format=btree
   application/x-bibtex-text-file
   application/x-bplist
   application/x-bplist-webarchive
   application/x-bzip2
   application/x-compress
   application/x-dvi
   application/x-elc
   application/x-endnote-refer
   application/x-executable
   application/x-font-otf
   application/x-font-ttf
   application/x-font-type1
   application/x-gtar
   application/x-hdf
   application/x-java-jnilib
   application/x-lzip
   application/x-mach-o
   application/x-matroska
   application/x-msdownload
   application/x-msdownload; format=pe
   application/x-msdownload; format=pe32
   application/x-msdownload; format=pe64
   application/x-object
   application/x-parquet
   application/x-plist
   application/x-rar-compressed; version=4
   application/x-rpm
   application/x-sh
   application/x-sharedlib
   application/x-shockwave-flash
   application/x-sqlite3
   application/x-tar
   application/x-tex
   application/x-tika-msoffice
   application/x-tika-ooxml
   application/x-x509-cert; format=der
   application/x-x509-cert; format=pem
   application/x-x509-dsa-parameters
   application/x-x509-key; format=der
   application/x-x509-key; format=pem
   application/x-xz
   application/xhtml+xml
   application/xml
   application/xslt+xml
   application/zip
   application/zlib
   application/zstd
   audio/mpeg
   audio/vnd.wave
   audio/vorbis
   audio/x-aiff
   image/aces
   image/bmp
   image/gif
   image/icns
   image/jpeg
   image/jxl
   image/png
   image/svg+xml
   image/tiff
   image/vnd.adobe.photoshop
   image/vnd.microsoft.icon
   image/vnd.zbrush.pcx
   image/webp
   image/x-3ds
   image/x-freehand
   image/x-portable-bitmap
   image/x-portable-graymap
   image/x-portable-pixmap
   image/x-raw-canon
   image/x-tga
   image/x-xcf
   message/rfc822
   text/calendar
   text/html
   text/plain
   text/troff
   text/vnd.graphviz
   text/x-awk
   text/x-chdr
   text/x-csrc
   text/x-diff
   text/x-jsp
   text/x-makefile
   text/x-matlab
   text/x-perl
   text/x-php
   text/x-python
   text/x-robots
   video/mp4
   video/mpeg
   video/quicktime


------------------------------------------------------------------------------------
                             Indexed content types
------------------------------------------------------------------------------------
   application/dicom
   application/gzip
   application/javascript
   application/mathematica
   application/mbox
   application/msword
   application/ogg
   application/pdf
   application/postscript
   application/rtf
   application/warc
   application/wasm
   application/xml
   application/zip
   application/zlib
   application/zstd
   message/rfc822
   text/calendar
   text/html
   text/plain
   text/troff


------------------------------------------------------------------------------------
                             Ignored content types
------------------------------------------------------------------------------------
   audio/mpeg
   audio/vorbis
   image/aces
   image/bmp
   image/gif
   image/icns
   image/jpeg
   image/jxl
   image/png
   image/tiff
   image/webp
   video/mp4
   video/mpeg
   video/quicktime
```

Example of searching:
```
➜ java -jar target/disksearch-1.1-SNAPSHOT.jar search
? 
```
