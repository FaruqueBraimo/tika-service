package tika.legacy;

import java.util.*;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.ocr.TesseractOCRConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import tika.model.TikaProcessingResult;
import tika.processor.AbstractTikaProcessor;
import javax.annotation.PostConstruct;


@Component("legacyTikaProcessor")
public class LegacyTikaProcessor extends AbstractTikaProcessor {

    @Autowired
    private LegacyPdfProcessorConfig config;

    private AutoDetectParser defaultParser;
    private ParseContext defaultParseContext;

    private Logger log = LoggerFactory.getLogger(LegacyTikaProcessor.class);


    @PostConstruct
    public void init() {
        defaultParseContext = new ParseContext();
        defaultParseContext.set(TikaConfig.class, config.getTikaConfig());
        defaultParseContext.set(LegacyPdfProcessorConfig.class, config);

        TesseractOCRConfig tessConfig = new TesseractOCRConfig();
        tessConfig.setTimeout(config.getOcrTimeout());
        defaultParseContext.set(TesseractOCRConfig.class, tessConfig);

        ImageMagickConfig imgConfig = new ImageMagickConfig();
        imgConfig.setTimeout(config.getConversionTimeout());
        defaultParseContext.set(ImageMagickConfig.class, imgConfig);

        defaultParser = new AutoDetectParser(config.getTikaConfig());
    }

    protected TikaProcessingResult processStream(TikaInputStream stream) {
        TikaProcessingResult result;

        try {
            BodyContentHandler handler = new BodyContentHandler();
            Metadata metadata = new Metadata();

            defaultParser.parse(stream, handler, metadata, defaultParseContext);

            // parse the metadata and store the result
            Map<String, Object> resultMetadata = extractMetadata(metadata);
            result = TikaProcessingResult.builder()
                    .text(handler.toString())
                    .metadata(resultMetadata)
                    .success(true)
                    .build();
        }
        catch (Exception e) {
            log.error(e.getMessage());

            result = TikaProcessingResult.builder()
                    .error("Exception caught while processing the document: " + e.getMessage())
                    .success(false)
                    .build();
        }

        return result;
    }
}