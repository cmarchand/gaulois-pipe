package fr.efl.chaine.xslt.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Saxon pipe URI resolver.
 */
public class GauloisPipeURIResolver implements URIResolver {

    private final URIResolver defaultUriResolver;

    /**
     * Logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GauloisPipeURIResolver.class);
    /**
     * Unix double slash.
     */
    private static final String UNIX_DOUBLE_SLASH = "//";
    /**
     * Unix simple slash.
     */
    private static final String UNIX_SIMPLE_SLASH = "/";
    /**
     * Empty string.
     */
    private static final String EMPTY_STRING = "";
    /**
     * Unix file protocol.
     */
    private static final String UNIX_FILE_PROTOCOL = "file:/";
    /**
     * Windows file protocol.
     */
    private static final String WINDOWS_FILE_PROTOCOL = "file:\\";
    /**
     * uri mapping.
     */
    private final Map<String, String> uriMapping;
    private final Map<Resolvee, File> alreadyResolved;

    /**
     * @param defaultUriResolver Saxon default URIResolver
     * Default constructor.
     */
    public GauloisPipeURIResolver(final URIResolver defaultUriResolver) {
        this(defaultUriResolver, Collections.EMPTY_MAP);
    }

    /**
     * Constructor with uri resolver mapping.
     *
     * @param defaultUriResolver Saxon default URIResolver
     * @param uriMapping the uri resolver mapping
     */
    public GauloisPipeURIResolver(final URIResolver defaultUriResolver, Map<String, String> uriMapping) {
        super();
        this.defaultUriResolver = defaultUriResolver;
        this.uriMapping = new HashMap<>(uriMapping);
        this.alreadyResolved = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     *
     * @param href
     * @param base
     * @return
     * @throws javax.xml.transform.TransformerException
     */
    @Override
    public Source resolve(String href, String base) throws TransformerException {
        Resolvee resolvee = new Resolvee(href, base);
        LOGGER.info("URIResolver on href=\"{}\" and base=\"{}\"", href, base);
        File ret = alreadyResolved.get(resolvee);
        if(ret != null) {
            try {
                StreamSource s = new StreamSource(new FileInputStream(ret));
                s.setSystemId(ret);
                LOGGER.debug("resolved from already served");
            } catch(FileNotFoundException ex) {}
        }
        try {
            if ("".equals(href)) {
                return defaultUriResolver.resolve(href, base);
            } else {
                String path;
                File file;
                try {
                    URI uri = new URI(href);
                    file = new File(uri);
                }
                catch (URISyntaxException | IllegalArgumentException | NullPointerException e) {
                    file = new File(href);
                }
                String filename = href.substring(href.lastIndexOf('/') + 1);
                if (uriMapping.containsKey(filename)) {
                    LOGGER.debug("Target file {} from uriMapping {}", filename, uriMapping.get(filename));
                    file = new File(uriMapping.get(filename));
                } else {
                    if (System.getProperty(filename) != null) {
                        LOGGER.debug("Target file {} from property {}", filename, System.getProperty(filename));
                        file = new File(System.getProperty(filename));
                    } else {
                        if (!file.exists()) {
                            if (base == null || base.isEmpty()) {
                                path = href;
                            } else {
                                File fBase = base.contains(":") ? new File(new URI(base)) : new File(base);
                                if(fBase.isFile()) fBase = fBase.getParentFile();
                                path = fBase.getAbsolutePath() + File.separator + href;
                            }
                            path = path.replace(UNIX_FILE_PROTOCOL, UNIX_SIMPLE_SLASH);
                            path = path.replace(WINDOWS_FILE_PROTOCOL, EMPTY_STRING);
                            LOGGER.debug("Try local file {}", path);
                            file = new File(path);
                        }
                        if (!file.exists()) {
                            LOGGER.debug("Not found");
                            LOGGER.debug("Try local file {} in {}", filename, System.getProperty("user.dir"));
                            File localFile = new File(filename);
                            if (localFile.exists()) {
                                file = localFile;
                            } else {
                                LOGGER.debug("Local file {} not found in {}", filename, System.getProperty("user.dir"));
                                LOGGER.debug("Try local file {} in {}", filename, System.getProperty("basedir"));
                                File basedirFile = new File(System.getProperty("basedir"), filename);
                                if (basedirFile.exists()) {
                                    file = basedirFile;
                                } else {
                                    LOGGER.debug("Local file {} not found in {}", filename, System.getProperty("basedir"));
                                }
                            }
                        }
                    }
                }
                if (file.exists()) {
                    LOGGER.debug("Resource found href=\"{}\" and base=\"{}\" is \"{}\"",
                            new String[]{href, base, file.getPath()});
                    StreamSource s = new StreamSource(new FileInputStream(file));
                    s.setSystemId(file);
                    alreadyResolved.put(resolvee, file);
                    return s;
                } else {
                    LOGGER.warn("Resource not found href=\"{}\", base=\"{}\", filename=\"{}\"",
                            new String[]{href, base, filename});
                    return null;
                }
            }
        } catch (FileNotFoundException | URISyntaxException e) {
            LOGGER.error(e.getMessage(), e);
            throw new TransformerException(e);
        }
    }
    
    public class Resolvee implements Serializable {
        private final String href, base;
        public Resolvee(final String href, final String base) {
            super();
            this.href = href;
            this.base = base;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 61 * hash + Objects.hashCode(this.href);
            hash = 61 * hash + Objects.hashCode(this.base);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Resolvee other = (Resolvee) obj;
            if (!Objects.equals(this.href, other.href)) {
                return false;
            }
            return Objects.equals(this.base, other.base);
        }
        
    }
}
