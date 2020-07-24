package com.alfresco.content.mimetype

enum class MimeType(val icon: Int) {
    ARCHIVE(R.drawable.file_ic_archive),
    AUDIO(R.drawable.file_ic_audio),
    BOOK(R.drawable.file_ic_book),
    CODE(R.drawable.file_ic_code),
    DATABASE(R.drawable.file_ic_database),
    EMAIL(R.drawable.file_ic_email),
    DOC_OTHER(R.drawable.file_ic_document),
    FORM(R.drawable.file_ic_form),
    IMAGE(R.drawable.file_ic_image),
    MS_DOC(R.drawable.file_ic_ms_document),
    MS_PPT(R.drawable.file_ic_ms_presentation),
    MS_SHEET(R.drawable.file_ic_ms_spreadsheet),
    PDF(R.drawable.file_ic_pdf),
    PPT_OTHER(R.drawable.file_ic_presentation),
    PROCESS(R.drawable.file_ic_process),
    SHEET_OTHER(R.drawable.file_ic_spreadsheet),
    TASK(R.drawable.file_ic_task),
    VIDEO(R.drawable.file_ic_video),

    OTHER(R.drawable.file_ic_generic),
    FOLDER(R.drawable.file_ic_folder),
    LIBRARY(R.drawable.file_ic_library);

    companion object {
        private val map by lazy {
            mapOf(
                "application/acp" to ARCHIVE,
                "application/dita+xml" to CODE,
                "application/eps" to IMAGE,
                "application/framemaker" to DOC_OTHER,
                "application/illustrator" to IMAGE,
                "application/java" to OTHER,
                "application/java-archive" to ARCHIVE,
                "application/json" to CODE,
                "application/mac-binhex40" to OTHER,
                "application/msword" to MS_DOC,
                "application/octet-stream" to OTHER,
                "application/oda" to DOC_OTHER,
                "application/ogg" to AUDIO,
                "application/pagemaker" to DOC_OTHER,
                "application/pdf" to PDF,
                "application/postscript" to IMAGE,
                "application/remote-printing" to DOC_OTHER,
                "application/rss+xml" to CODE,
                "application/rtf" to DOC_OTHER,
                "application/sgml" to CODE,
                "application/vnd.adobe.aftereffects.project" to OTHER,
                "application/vnd.adobe.aftereffects.template" to OTHER,
                "application/vnd.adobe.air-application-installer-package+zip" to ARCHIVE,
                "application/vnd.adobe.xdp+xml" to OTHER,
                "application/vnd.android.package-archive" to ARCHIVE,
                "application/vnd.apple.keynote" to PPT_OTHER,
                "application/vnd.apple.numbers" to SHEET_OTHER,
                "application/vnd.apple.pages" to DOC_OTHER,
                "application/vnd.ms-excel" to MS_SHEET,
                "application/vnd.ms-excel.addin.macroenabled.12" to MS_SHEET,
                "application/vnd.ms-excel.sheet.binary.macroenabled.12" to MS_SHEET,
                "application/vnd.ms-excel.sheet.macroenabled.12" to MS_SHEET,
                "application/vnd.ms-excel.template.macroenabled.12" to MS_SHEET,
                "application/vnd.ms-outlook" to EMAIL,
                "application/vnd.ms-powerpoint" to MS_PPT,
                "application/vnd.ms-powerpoint.addin.macroenabled.12" to MS_PPT,
                "application/vnd.ms-powerpoint.presentation.macroenabled.12" to MS_PPT,
                "application/vnd.ms-powerpoint.slide.macroenabled.12" to MS_PPT,
                "application/vnd.ms-powerpoint.slideshow.macroenabled.12" to MS_PPT,
                "application/vnd.ms-powerpoint.template.macroenabled.12" to MS_PPT,
                "application/vnd.ms-project" to PROCESS,
                "application/vnd.ms-word.document.macroenabled.12" to MS_DOC,
                "application/vnd.ms-word.template.macroenabled.12" to MS_DOC,
                "application/vnd.oasis.opendocument.chart" to OTHER,
                "application/vnd.oasis.opendocument.database" to DATABASE,
                "application/vnd.oasis.opendocument.formula" to OTHER,
                "application/vnd.oasis.opendocument.graphics" to IMAGE,
                "application/vnd.oasis.opendocument.graphics-template" to IMAGE,
                "application/vnd.oasis.opendocument.image" to IMAGE,
                "application/vnd.oasis.opendocument.presentation" to PPT_OTHER,
                "application/vnd.oasis.opendocument.presentation-template" to PPT_OTHER,
                "application/vnd.oasis.opendocument.spreadsheet" to SHEET_OTHER,
                "application/vnd.oasis.opendocument.spreadsheet-template" to SHEET_OTHER,
                "application/vnd.oasis.opendocument.text" to DOC_OTHER,
                "application/vnd.oasis.opendocument.text-master" to DOC_OTHER,
                "application/vnd.oasis.opendocument.text-template" to DOC_OTHER,
                "application/vnd.oasis.opendocument.text-web" to DOC_OTHER,
                "application/vnd.openxmlformats-officedocument.presentationml.presentation" to PPT_OTHER,
                "application/vnd.openxmlformats-officedocument.presentationml.slide" to PPT_OTHER,
                "application/vnd.openxmlformats-officedocument.presentationml.slideshow" to PPT_OTHER,
                "application/vnd.openxmlformats-officedocument.presentationml.template" to PPT_OTHER,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet" to SHEET_OTHER,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.template" to SHEET_OTHER,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document" to DOC_OTHER,
                "application/vnd.openxmlformats-officedocument.wordprocessingml.template" to DOC_OTHER,
                "application/vnd.stardivision.calc" to SHEET_OTHER,
                "application/vnd.stardivision.chart" to OTHER,
                "application/vnd.stardivision.draw" to IMAGE,
                "application/vnd.stardivision.impress" to PPT_OTHER,
                "application/vnd.stardivision.impress-packed" to PPT_OTHER,
                "application/vnd.stardivision.math" to OTHER,
                "application/vnd.stardivision.writer" to DOC_OTHER,
                "application/vnd.stardivision.writer-global" to DOC_OTHER,
                "application/vnd.sun.xml.calc" to SHEET_OTHER,
                "application/vnd.sun.xml.calc.template" to SHEET_OTHER,
                "application/vnd.sun.xml.draw" to IMAGE,
                "application/vnd.sun.xml.impress" to PPT_OTHER,
                "application/vnd.sun.xml.impress.template" to PPT_OTHER,
                "application/vnd.sun.xml.writer" to DOC_OTHER,
                "application/vnd.sun.xml.writer.template" to DOC_OTHER,
                "application/vnd.visio" to PROCESS,
                "application/vnd.visio2013" to PROCESS,
                "application/wordperfect" to DOC_OTHER,
                "application/x-bcpio" to ARCHIVE,
                "application/x-compress" to ARCHIVE,
                "application/x-cpio" to ARCHIVE,
                "application/x-csh" to IMAGE,
                "application/x-dosexec" to OTHER,
                "application/x-dvi" to DOC_OTHER,
                "application/x-fla" to VIDEO,
                "application/x-gtar" to ARCHIVE,
                "application/x-gzip" to ARCHIVE,
                "application/x-hdf" to ARCHIVE,
                "application/x-indesign" to IMAGE,
                "application/x-javascript" to CODE,
                "application/x-latex" to DOC_OTHER,
                "application/x-mif" to DOC_OTHER,
                "application/x-netcdf" to DOC_OTHER,
                "application/x-rar-compressed" to ARCHIVE,
                "application/x-sh" to CODE,
                "application/x-shar" to ARCHIVE,
                "application/x-shockwave-flash" to VIDEO,
                "application/x-sv4cpio" to ARCHIVE,
                "application/x-sv4crc" to ARCHIVE,
                "application/x-tar" to ARCHIVE,
                "application/x-tcl" to CODE,
                "application/x-tex" to DOC_OTHER,
                "application/x-texinfo" to DOC_OTHER,
                "application/x-troff" to OTHER,
                "application/x-troff-man" to OTHER,
                "application/x-troff-me" to OTHER,
                "application/x-troff-mes" to OTHER,
                "application/x-ustar" to ARCHIVE,
                "application/x-wais-source" to CODE,
                "application/x-x509-ca-cert" to OTHER,
                "application/x-zip" to ARCHIVE,
                "application/xhtml+xml" to CODE,
                "application/zip" to ARCHIVE,
                "audio/basic" to AUDIO,
                "audio/mp4" to AUDIO,
                "audio/mpeg" to AUDIO,
                "audio/ogg" to AUDIO,
                "audio/vnd.adobe.soundbooth" to AUDIO,
                "audio/vorbis" to AUDIO,
                "audio/x-aiff" to AUDIO,
                "audio/x-flac" to AUDIO,
                "audio/x-ms-wma" to AUDIO,
                "audio/x-wav" to AUDIO,
                "image/bmp" to IMAGE,
                "image/cgm" to IMAGE,
                "image/gif" to IMAGE,
                "image/ief" to IMAGE,
                "image/jp2" to IMAGE,
                "image/jpeg" to IMAGE,
                "image/png" to IMAGE,
                "image/svg+xml" to IMAGE,
                "image/tiff" to IMAGE,
                "image/vnd.adobe.photoshop" to IMAGE,
                "image/vnd.adobe.premiere" to VIDEO,
                "image/vnd.dwg" to OTHER,
                "image/x-cmu-raster" to IMAGE,
                "image/x-dwt" to OTHER,
                "image/x-portable-anymap" to IMAGE,
                "image/x-portable-bitmap" to IMAGE,
                "image/x-portable-graymap" to IMAGE,
                "image/x-portable-pixmap" to IMAGE,
                "image/x-raw-adobe" to IMAGE,
                "image/x-raw-canon" to IMAGE,
                "image/x-raw-fuji" to IMAGE,
                "image/x-raw-hasselblad" to IMAGE,
                "image/x-raw-kodak" to IMAGE,
                "image/x-raw-leica" to IMAGE,
                "image/x-raw-minolta" to IMAGE,
                "image/x-raw-nikon" to IMAGE,
                "image/x-raw-olympus" to IMAGE,
                "image/x-raw-panasonic" to IMAGE,
                "image/x-raw-pentax" to IMAGE,
                "image/x-raw-red" to IMAGE,
                "image/x-raw-sigma" to IMAGE,
                "image/x-raw-sony" to IMAGE,
                "image/x-rgb" to IMAGE,
                "image/x-xbitmap" to IMAGE,
                "image/x-xpixmap" to IMAGE,
                "image/x-xwindowdump" to IMAGE,
                "message/rfc822" to EMAIL,
                "text/calendar" to OTHER,
                "text/css" to CODE,
                "text/csv" to SHEET_OTHER,
                "text/html" to CODE,
                "text/mediawiki" to CODE,
                "text/plain" to DOC_OTHER,
                "text/richtext" to DOC_OTHER,
                "text/sgml" to CODE,
                "text/tab-separated-values" to SHEET_OTHER,
                "text/x-java-source" to CODE,
                "text/x-jsp" to CODE,
                "text/x-markdown" to CODE,
                "text/x-setext" to OTHER,
                "text/xml" to CODE,
                "video/3gpp" to VIDEO,
                "video/3gpp2" to VIDEO,
                "video/mp2t" to VIDEO,
                "video/mp4" to VIDEO,
                "video/mpeg" to VIDEO,
                "video/mpeg2" to VIDEO,
                "video/ogg" to VIDEO,
                "video/quicktime" to VIDEO,
                "video/webm" to VIDEO,
                "video/x-flv" to VIDEO,
                "video/x-m4v" to VIDEO,
                "video/x-ms-asf" to VIDEO,
                "video/x-ms-wmv" to VIDEO,
                "video/x-msvideo" to VIDEO,
                "video/x-rad-screenplay" to VIDEO,
                "video/x-sgi-movie" to VIDEO,
                "x-world/x-vrml" to OTHER
            )
        }

        fun with(type: String?): MimeType {
            return map[type] ?: OTHER
        }
    }
}
