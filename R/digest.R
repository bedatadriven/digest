
##  digest -- hash digest functions for R
##
##  Copyright (C) 2003 - 2015  Dirk Eddelbuettel <edd@debian.org>
##
##  This file is part of digest.
##
##  digest is free software: you can redistribute it and/or modify
##  it under the terms of the GNU General Public License as published by
##  the Free Software Foundation, either version 2 of the License, or
##  (at your option) any later version.
##
##  digest is distributed in the hope that it will be useful,
##  but WITHOUT ANY WARRANTY; without even the implied warranty of
##  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
##  GNU General Public License for more details.
##
##  You should have received a copy of the GNU General Public License
##  along with digest.  If not, see <http://www.gnu.org/licenses/>.


digest <- function(object, algo=c("md5", "sha1", "crc32", "sha256", "sha512",
                           "xxhash32", "xxhash64", "murmur32"),
                   serialize=TRUE, file=FALSE, length=Inf,
                   skip="auto", ascii=FALSE, raw=FALSE, seed=0,
                   errormode=c("stop","warn","silent")) {
    
    algo <- match.arg(algo)
    errormode <- match.arg(errormode)

    .errorhandler <- function(txt, obj="", mode="stop") {
        if (mode == "stop") {
            stop(txt, obj, call.=FALSE)
        } else if (mode == "warn") {
            warning(txt, obj, call.=FALSE)
            return(invisible(NA))
        } else {
            return(invisible(NULL))
        }
    }
    
    if (is.infinite(length)) {
        length <- -1               # internally we use -1 for infinite len
    }

    if (is.character(file) && missing(object)) {
        object <- file
        file <- TRUE
    }

    if (serialize && !file) {
        return(Digest$hashSerializedSexp(object, algo, ascii, raw, skip))
      
    } else if (!is.character(object) && !inherits(object,"raw")) {
        return(.errorhandler(paste("Argument object must be of type character",
                                      "or raw vector if serialize is FALSE"), mode=errormode))
    }
    if (file && !is.character(object))
        return(.errorhandler("file=TRUE can only be used with a character object",
                             mode=errormode))
  
    if (file) {
        object <- path.expand(object)
        if (!file.exists(object)) {
            return(.errorhandler("The file does not exist: ", object, mode=errormode))
        }
        if (!isTRUE(!file.info(object)$isdir)) {
            return(.errorhandler("The specified pathname is not a file: ",
                                 object, mode=errormode))
        }
        if (file.access(object, 4)) {
            return(.errorhandler("The specified file is not readable: ",
                                 object, mode=errormode))
        }
        return(Digest$hashFile(object, algo, raw, skip));
    
    } else {
        return(Digest$hashVector(object, algo, raw, skip))
    }
}
