package com.codeinvestigator.springbootsftpmessagehandler.download;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.Poller;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundFileSynchronizingMessageSource;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.MessageHandler;

import java.io.File;
import java.io.IOException;

@Configuration
@Slf4j
class SftpConfig {

    public DefaultSftpSessionFactory gimmeFactory(){
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory();
        factory.setHost("0.0.0.0");
        factory.setPort(22);
        factory.setAllowUnknownKeys(true);
        factory.setUser("mike");
        factory.setPassword("password123");
        return factory;
    }

    @Bean(name="mydefaultsync")
    public SftpInboundFileSynchronizer synchronizer(){
        SftpInboundFileSynchronizer sync = new SftpInboundFileSynchronizer(gimmeFactory());
        sync.setDeleteRemoteFiles(false);
        sync.setRemoteDirectory("/upload/done");
        sync.setFilter(new SftpPersistentAcceptOnceFileListFilter(metadataStore(),""));
        return sync;
    }

    @Bean(name="sftpMessageSource")
    @InboundChannelAdapter(channel="fileuploaded", poller = @Poller(fixedDelay = "3000"))
    public MessageSource<File> sftpMessageSource(){
        SftpInboundFileSynchronizingMessageSource source =
                new SftpInboundFileSynchronizingMessageSource(synchronizer());
        source.setLocalDirectory(new File("tmp/incoming"));
        source.setAutoCreateLocalDirectory(true);
        //source.setLocalFilter(new SftpPersistentAcceptOnceFileListFilter(metadataStore(),""));
        //source.setMaxFetchSize(1);
        return source;
    }

    @Bean(name="fileStore")
    public PropertiesPersistingMetadataStore metadataStore() {
        PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
        metadataStore.setBaseDirectory("filestore");
        metadataStore.setFileName("filestore.properties");
        metadataStore.afterPropertiesSet();
        return metadataStore;
    }


    @ServiceActivator(inputChannel = "fileuploaded")
    public void handleIncomingFile(File file) throws IOException {
        log.info(String.format("handleIncomingFile BEGIN %s", file.getName()));
        String content = FileUtils.readFileToString(file, "UTF-8");
        log.error(String.format("Content: %s", content));
        log.error(String.format("handleIncomingFile END %s", file.getName()));
         file.delete();
    }


}
