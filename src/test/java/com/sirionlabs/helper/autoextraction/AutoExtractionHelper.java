package com.sirionlabs.helper.autoextraction;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.opencsv.CSVReader;
import com.sirionlabs.api.autoExtraction.API.CDRShow;
import com.sirionlabs.api.autoExtraction.ContractShow;
import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.config.ConfigureEnvironment;
import com.sirionlabs.helper.ListRenderer.ListDataHelper;
import com.sirionlabs.utils.commonUtils.APIUtils;
import com.sirionlabs.utils.commonUtils.CustomAssert;
import org.apache.commons.configuration2.EnvironmentConfiguration;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.protocol.types.Field;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.util.Strings;

import java.io.*;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class AutoExtractionHelper {
    private final static Logger logger = LoggerFactory.getLogger(AutoExtractionHelper.class);
    static long duration = 0;
    static APIUtils apiUtils = new APIUtils();

    public static HttpResponse hitFetchModelInfoAPI(HttpHost httpHost, String algoid) {
        HttpResponse response = null;
        HttpClient httpClient;
        httpClient = HttpClientBuilder.create().build();

        HttpGet getRequest;
        try {
            String queryString = "/autoExtraction/fetchModelInfo/" + algoid;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            response = httpClient.execute(httpHost, getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Fetch Model API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Fetch Model API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitTabdataAPI(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse autoExtractionColumnListingAPI(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse metadataFilterAPI(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse updateProperties(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse serviceLevelExtraction(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse aeDocumentReset(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse aeInsights(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse localSearch(String payload) {
        String query = "/listRenderer/list/432/listdata";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Local Search API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse docShowPage(int recordId) {
        String query = "/autoextraction/document/" + recordId + "/metadata";
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            getRequest = new HttpGet(query);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get Document showpage header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Document Show page API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getTabData(String payload, int listId) {

        String query = "/listRenderer/list/" + listId + "/listdata?isFirstCall=false";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause tab List data API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Clause tab list data API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getAuditLog(String payload, String recordId) {

        String query = "/listRenderer/list/61/tablistdata/316/" + recordId + "?version=2.0";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Audit log tab List data API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Audit log tab list data API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse globalSingleEntitySearch(int docId) {
        String query = "/autoextraction/getEntityId/" + docId + "";
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            getRequest = new HttpGet(query);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get global Search header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting global Search  API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse globalMultipleEntitySearch(String payload, int listId) {

        String query = "/listRenderer/list/" + listId + "/listdata";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug(" List data API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting global Multiple Entity Search data API. {}", e.getMessage());
        }
        return response;
    }

    private static Consumer<Long, String> createConsumer(String serverDetails, String topicName) {
        final Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                serverDetails);
        props.put(ConsumerConfig.GROUP_ID_CONFIG,
                "KafkaExampleConsumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                LongDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class.getName());
        // Create the consumer using props.
        final Consumer<Long, String> consumer =
                new KafkaConsumer<>(props);
        // Subscribe to the topic.
        consumer.subscribe(Collections.singletonList(topicName));
        return consumer;
    }

    public static List<String> runConsumer(String serverDetails, String topicName) throws InterruptedException {
        List<String> latestRecords = new LinkedList<>();
        final Consumer<Long, String> consumer = createConsumer(serverDetails, topicName);
        final int giveUp = 10;
        int noRecordsCount = 0;
        while (true) {
            Thread.sleep(2000);
            final ConsumerRecords<Long, String> consumerRecords =
                    consumer.poll(1000);
            if (consumerRecords.count() == 0) {
                noRecordsCount++;
                if (noRecordsCount > giveUp) break;
                else {
                    continue;
                }
            }

            consumerRecords.forEach(record -> {
                System.out.printf("Consumer Record:(%d, %s, %d, %d)\n",
                        record.key(), record.value(),
                        record.partition(), record.offset());
                latestRecords.add(record.value());
            });
            consumer.commitAsync();
        }
        consumer.close();
        System.out.println("DONE");
        return latestRecords;
    }

    public static HttpResponse getListingData(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            if (payload != null)
                postRequest.setEntity(new StringEntity(payload, "UTF-8"));

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static Boolean getDocumentFromDocumentViewer(String outputFilePath, String outputFileName, int documentId) {
        Boolean fileDownloaded = false;
        String query = "/metadataautoextraction/downloadDocument/" + documentId;
        String acceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3";
        APIUtils apiUtils = new APIUtils();
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            HttpHost target = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("Host"), Integer.valueOf(ConfigureEnvironment.getEnvironmentProperty("Port")), ConfigureEnvironment.getEnvironmentProperty("Scheme"));
            HttpGet httpGet = apiUtils.generateHttpGetRequestWithQueryString(query, acceptHeader);
            fileDownloaded = apiUtils.downloadAPIResponseFile(outputFilePath, outputFileName, target, httpGet);
        } catch (Exception e) {
            logger.error("Exception while hitting Document Download From Contract Document tab CDR API. {}", e.getMessage());
        }
        return fileDownloaded;
    }


    public static void deleteAllFilesFromDirectory(String directory) {
        File file = new File(directory);
        String[] myFiles;
        if (file.isDirectory()) {
            myFiles = file.list();
            for (int i = 0; i < myFiles.length; i++) {
                File myFile = new File(file, myFiles[i]);
                if (myFile.getName().contains("txt")) {
                    System.out.println("Ignoring text file");
                } else {
                    myFile.delete();
                }
            }
        }
    }

    public static HttpResponse checkAutoExtractionDocListingMetaData(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse checkDataforAllDocuments(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryPath = query;
            logger.debug("Query string url formed is {}", queryPath);
            postRequest = new HttpPost(queryPath);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }


    public static HttpResponse getAndCheckMetaDataForAE645(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse downloadDocViewer(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitShowViewerAPI(String query) {
        HttpResponse response = null;

        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse saveParsingJobInfoAPI(HttpHost httpHost, String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        HttpClient httpClient = HttpClientBuilder.create().build();
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            if (payload != null)
                postRequest.setEntity(new StringEntity(payload, "UTF-8"));

            response = httpClient.execute(httpHost, postRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getfetchDocumentInfo(HttpHost httpHost, String query) {
        HttpResponse response = null;
        HttpClient httpClient;
        httpClient = HttpClientBuilder.create().build();

        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            response = httpClient.execute(httpHost, getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse updateAccessCriteria(String query, String params) {
        HttpResponse response = null;
        HttpClient httpClient;
        httpClient = HttpClientBuilder.create().build();

        HttpHost httpHost = new HttpHost(ConfigureEnvironment.getEnvironmentProperty("host"), Integer.valueOf(ConfigureEnvironment.getEnvironmentProperty("port")), ConfigureEnvironment.getEnvironmentProperty("scheme"));
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);

            HttpPost httpPostRequest = apiUtils.generateHttpPostRequestWithQueryStringAndPayload(queryString, "text/html, */*; q=0.01", "application/x-www-form-urlencoded; charset=UTF-8", params);
            response = httpClient.execute(httpHost, httpPostRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse checkFieldTypeDataType(String query, String payload) {

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;

    }


    private boolean downloadFileFromResponse(String outputFilePath, String outputFileName, HttpHost target, String queryString, String acceptHeader) {
        boolean fileDownloaded = false;
        try {
            APIUtils apiUtils = new APIUtils();
            HttpGet getRequest = apiUtils.generateHttpGetRequestWithQueryString(queryString, acceptHeader);
            fileDownloaded = apiUtils.downloadAPIResponseFile(outputFilePath, outputFileName, target, getRequest);
        } catch (Exception e) {
            logger.error("Exception while hitting Document Fetch API", e.getMessage());
        }
        return fileDownloaded;
    }

    private static byte[] convertPDFToByteArray(String filePath) {

        InputStream inputStream = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            inputStream = new FileInputStream(filePath);
            byte[] buffer = new byte[1024];
            baos = new ByteArrayOutputStream();
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return baos.toByteArray();
    }

    public void saveKafkaTopicDataInFile(String hosturl, String userName, String password, String kafkaConsumerCommand, String testFile) {
        String command1 = "cd /tomcat/kafka/bin";
        String command2 = "source /etc/profile";
        String command3 = kafkaConsumerCommand + " > " + testFile;
        try {
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            JSch jsch = new JSch();
            Session session = jsch.getSession(userName, hosturl, 22);
            session.setPassword(password);
            session.setConfig(config);
            session.connect();
            if (session.isConnected()) {
                System.out.println("Connected");
            }

            Channel channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command1 + ';' + command2 + ";" + command3);
            ((ChannelExec) channel).setErrStream(System.err);

            while (!channel.isConnected()) {
                channel.connect(20000);
            }
            Thread.sleep(10000);

            channel.disconnect();
            session.disconnect();
            System.out.println("DONE");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> readDataFromFileFetchedFromRemoteServer(String filePath) {
        List<String> dataFromKafkaTopic = new LinkedList<>();
        BufferedReader reader;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line = reader.readLine();
            while (line != null) {
                dataFromKafkaTopic.add(line);
                // read next line
                line = reader.readLine();
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return dataFromKafkaTopic;
    }

    public static HttpResponse hitSaveModelUploadAPI(String aeHostUrl, int aePort, String query, HttpEntity httpEntity) {
        HttpResponse response = null;
        HttpClient httpClient;
        if (ConfigureEnvironment.isProxyEnabled) {
            HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
            httpClient = HttpClients.custom().setProxy(proxy).build();
        } else {
            httpClient = HttpClientBuilder.create().build();
        }
        HttpHost httpHost = new HttpHost(aeHostUrl, aePort, "http");
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.setEntity(httpEntity);

            response = httpClient.execute(httpHost, postRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitSaveDocumentStatusAPI(String aeHostUrl, int aePort, String query, String payload) {
        HttpResponse response = null;
        HttpClient httpClient;
        if (ConfigureEnvironment.isProxyEnabled) {
            HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
            httpClient = HttpClients.custom().setProxy(proxy).build();
        } else {
            httpClient = HttpClientBuilder.create().build();
        }
        HttpHost httpHost = new HttpHost(aeHostUrl, aePort, "http");
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            if (payload != null)
                postRequest.setEntity(new StringEntity(payload, "UTF-8"));

            response = httpClient.execute(httpHost, postRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitExtractedDataCategoryFieldsApi(String aeHostUrl, int aePort, String query) {
        HttpResponse response = null;
        HttpClient httpClient;
        httpClient = HttpClientBuilder.create().build();

        HttpHost httpHost = new HttpHost(aeHostUrl, aePort, "http");
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            response = httpClient.execute(httpHost, getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitGeneralTabAPI(int recordId) {
        HttpResponse response = null;

        HttpGet getRequest;
        try {
            String queryString = "/autoextraction/getGeneralData/" + recordId;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("General tab API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting general Tab API. {}", e.getMessage());
        }
        return response;
    }


    public static HttpResponse savedDashboard() {
        HttpResponse response = null;

        HttpGet getRequest;
        try {
            String queryString = "/autoextraction/savedDashboards";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Saved Dashboard API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Saved Dashboard API. {}", e.getMessage());
        }
        return response;
    }


    public static HttpResponse hitSaveExtractedInfoApi(String aeHostUrl, int aePort, String query, String payload) {
        HttpResponse response = null;
        HttpClient httpClient;
        if (ConfigureEnvironment.isProxyEnabled) {
            HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
            httpClient = HttpClients.custom().setProxy(proxy).build();
        } else {
            httpClient = HttpClientBuilder.create().build();
        }
        HttpHost httpHost = new HttpHost(aeHostUrl, aePort, "http");
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            if (payload != null)
                postRequest.setEntity(new StringEntity(payload, "UTF-8"));

            response = httpClient.execute(httpHost, postRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitFetchDocCatInfoApi(String ae2HostUrl, int ae2Port, String query, String payload) {
        HttpResponse response = null;
        HttpClient httpClient;
        if (ConfigureEnvironment.isProxyEnabled) {
            HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
            httpClient = HttpClients.custom().setProxy(proxy).build();
        } else {
            httpClient = HttpClientBuilder.create().build();
        }
        HttpHost httpHost = new HttpHost(ae2HostUrl, ae2Port, "http");
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json");
            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Cache-Control", "no-cache");
            postRequest.addHeader("accept-encoding", "gzip, deflate");

            if (payload != null)
                postRequest.setEntity(new StringEntity(payload, "UTF-8"));

            response = httpClient.execute(httpHost, postRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitFetchDocCatInfoAE1Config(String ae1HostUrl, int ae1Port, String query, String payload) {
        HttpResponse response = null;
        HttpClient httpClient;
        if (ConfigureEnvironment.isProxyEnabled) {
            HttpHost proxy = new HttpHost(ConfigureEnvironment.proxyServerHost, ConfigureEnvironment.proxyServerPort);
            httpClient = HttpClients.custom().setProxy(proxy).build();
        } else {
            httpClient = HttpClientBuilder.create().build();
        }
        HttpHost httpHost = new HttpHost(ae1HostUrl, ae1Port, "http");
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Content-Type", "application/json");
            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Cache-Control", "no-cache");
            postRequest.addHeader("accept-encoding", "gzip, deflate");

            if (payload != null)
                postRequest.setEntity(new StringEntity(payload, "UTF-8"));

            response = httpClient.execute(httpHost, postRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitFetchMetaDataApi(String ae2HostUrl, int ae2Port, String query) {
        HttpResponse response = null;
        HttpClient httpClient;
        httpClient = HttpClientBuilder.create().build();

        HttpHost httpHost = new HttpHost(ae2HostUrl, ae2Port, "http");
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            response = httpClient.execute(httpHost, getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Delegation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse projectListDataAPI(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            postRequest = new HttpPost(query);
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Project List Data API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Project List Data API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getAllMetaDataFields(String query) {
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            getRequest = new HttpGet(query);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get All Meta Data Fields API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getTableData(String query) {
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            getRequest = new HttpGet(query);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get All Meta Data Fields API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getRoleGroupsForAssignment() {
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = "/autoextraction/v1/stakeholder/316";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get All Meta Data Fields API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getStatusOfDocumentAPI(String query) {
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            getRequest = new HttpGet(query);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get All Meta Data Fields API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse inprogressDocs(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            postRequest = new HttpPost(query);
            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Project List Data API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Project List Data API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getDataOfallInsights(String query) {
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            getRequest = new HttpGet(query);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get Insights Data API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse categoryShowpage(String query) {
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            getRequest = new HttpGet(query);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get category showpage header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Category Show page API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getAllNonMappedFields(String query) {
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            getRequest = new HttpGet(query);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get All Meta Data Fields API. {}", e.getMessage());
        }
        return response;
    }


    public static HttpResponse createProject(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            postRequest = new HttpPost(query);
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Project List Data API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Project List Data API. {}", e.getMessage());
        }
        return response;
    }


    public static HttpResponse updateProject(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            postRequest = new HttpPost(query);
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Project List Data API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Project List Data API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse clauseDeviationListing(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            postRequest = new HttpPost(query);
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Project List Data API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Clause Deviation Listing List Data API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse ruleBased(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            postRequest = new HttpPost(query);
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Project List Data API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Clause Deviation Listing List Data API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse deviationHistory(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            postRequest = new HttpPost(query);
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Project List Data API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Clause Deviation Listing List Data API. {}", e.getMessage());
        }
        return response;
    }

    public static List<Map<String, String>> readFromCSV(String fileName) throws IOException {
        List<Map<String, String>> rowValues = new ArrayList<>();
        CSVReader reader = new CSVReader(new FileReader(fileName), ',');
        String[] header = reader.readNext();
        String nextRecord[];
        Map<String, String> csvRowValues;
        while ((nextRecord = reader.readNext()) != null) {
            csvRowValues = new HashMap<>();
            int count = 0;
            for (String rowValue : nextRecord) {
                csvRowValues.put(header[count++], rowValue);
            }
            rowValues.add(csvRowValues);
        }
        return rowValues;
    }

    public static HttpResponse categoryListingAPI(String categoryListDataQuery, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            logger.debug("Query string url formed is {}", categoryListDataQuery);
            postRequest = new HttpPost(categoryListDataQuery);
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Group Creation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Project List Data API. {}", e.getMessage());
        }
        return response;
    }


    public static HttpResponse categoryCreationAPI(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            postRequest = new HttpPost(query);
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Category Creation header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Category Creation API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse categoryUpdateAPI(String request, String requestBody) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            logger.debug("Query string url formed is {}", request);
            postRequest = new HttpPost(request);
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, requestBody);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Category Update API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Category Update API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse categoryNoField(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            postRequest = new HttpPost(query);
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Category with no field mapping API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Category Create API with no field mapping. {}", e.getMessage());
        }
        return response;
    }


    public static void readFromCSV(String fileName, String... columnNames) throws IOException {
        for (String column : columnNames) {
            readFromCSV(fileName).stream().filter(m -> m.get(column).length() > 0).
                    map(row -> row.get(column).trim()).collect(Collectors.toList());
        }

    }

    public static boolean getExtractionStatus(String endUserName, String endUserPassword) throws IOException, InterruptedException {
        boolean isExtractionCompleted = false;
        Check check = new Check();
        CustomAssert csAssert = new CustomAssert();
        check.hitCheck(endUserName, endUserPassword);
        LocalTime initialTime = LocalTime.now();
        Thread.sleep(20000);
        try {
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{}}";
            HttpResponse listingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
            csAssert.assertTrue(listingResponse.getStatusLine().getStatusCode() == 200, "Response code is Invalid");
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseJson = new JSONObject(listingResponseStr);
            Set<String> keys = listingResponseJson.getJSONArray("data").getJSONObject(0).keySet();
            try {
                for (String key : keys) {
                    if (listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("columnName").equals("status")) {
                        int status = Integer.valueOf(listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(key).get("value").toString().split(":;")[1]);
                        while (status != 4) {
                            LocalTime finalTime = LocalTime.now();
                            duration = duration + Duration.between(initialTime, finalTime).getSeconds();
                            logger.info("Waiting for Extraction to complete Wait Time = " + duration + " seconds");
                            if (duration > 600) {
                                Assert.fail("Extraction is working slow waited for 10 minutes.Please look manually whether their is problem in extraction or services are working slow." + "Waited for:" + duration + "seconds for the document completion");
                            } else {
                                isExtractionCompleted = getExtractionStatus(endUserName, endUserPassword);
                                if (isExtractionCompleted == true) {
                                    return isExtractionCompleted;
                                }
                            }
                        }
                        if (status == 4) {
                            isExtractionCompleted = true;
                            duration = 0;
                            logger.info("Extraction Completed");
                            return isExtractionCompleted;
                        }

                        break;
                    }


                }

            } catch (Exception e) {
                logger.error("Exception while hitting Project Listing API. {}", e.getMessage());

            }
        } catch (Exception e) {
            logger.error("Exception while hitting Automation List Data API. {}", e.getMessage());
        } finally {
            duration = 0;
        }
        return isExtractionCompleted;
    }


    public static String getSourceDocument() throws IOException, InterruptedException {
        CustomAssert csAssert = new CustomAssert();
        String actualSourceDocument = " ";
        try {
            String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0&isFirstCall=true";
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"438\":{\"filterId\":\"438\",\"filterName\":\"duplicatedocs\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"AssignedShowExists\"}]}}}},\"selectedColumns\":[]}";
            HttpResponse listingResponse = AutoExtractionHelper.autoExtractionColumnListingAPI(query, payload);
            csAssert.assertTrue(listingResponse.getStatusLine().getStatusCode() == 200, "Response code is Invalid");
            String listingResponseStr = EntityUtils.toString(listingResponse.getEntity());
            JSONObject listingResponseJson = new JSONObject(listingResponseStr);
            int columnId = ListDataHelper.getColumnIdFromColumnName(listingResponseStr, "sourcedocument");
            try {

                actualSourceDocument = listingResponseJson.getJSONArray("data").getJSONObject(0).getJSONObject(Integer.toString(columnId)).get("value").toString().trim().split(":;")[0];
            } catch (Exception e) {
                logger.error("Exception while getting source document from Listing API. {}", e.getMessage());

            }
        } catch (Exception e) {
            logger.error("Exception while hitting Automation List Data API. {}", e.getMessage());
        }
        return actualSourceDocument;
    }

    public static HttpResponse hitGroupOrTagCreationAndUpdatePropertiesAPI(String url, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            logger.debug("Query string url formed is {}", url);
            postRequest = new HttpPost(url);
            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Group Creation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Project List Data API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse hitMasterUserRoleGroup(String url, String roleGroupId) {
        HttpResponse response = null;
        HttpGet getRequest;
        String query = url + roleGroupId;
        try {
            logger.debug("Query string url formed is {}", query);
            getRequest = new HttpGet(query);
            getRequest.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get All Meta Data Fields API. {}", e.getMessage());
        }
        return response;
    }

    public static List<String> getIdsForEntityFromName(String masterDataStr, String entity) {
        List<String> ids = new LinkedList<>();
        JSONObject masterDataJson = new JSONObject(masterDataStr);
        int count = masterDataJson.getJSONArray(entity).length();
        for (int i = 0; i < count; i++) {
            ids.add(masterDataJson.getJSONArray(entity).getJSONObject(i).get("id").toString());
        }
        return ids;
    }

    public static boolean verifyEntityAddedInMasterData(List<String> entityIds, int generatedId) {
        boolean isEntityStoredInMasterData;
        if (entityIds.contains(String.valueOf(generatedId))) {
            isEntityStoredInMasterData = true;
            logger.info("Newly created entity Id is present in Master Data " + generatedId);
        } else {
            isEntityStoredInMasterData = false;
            logger.info("Newly created entity Id is not present in Master Data " + generatedId);
        }
        return isEntityStoredInMasterData;
    }

    public static HttpResponse hitExtractDocViewerDocId(int automationListingDocId) {
        HttpResponse response = null;

        HttpGet getRequest;
        try {
            String queryString = "/autoextraction/document/" + automationListingDocId + "/metadata";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            response = apiUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("AutoExtraction Document MetaData API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting AutoExtraction Document MetaData API. {}", e.getMessage());
        }
        return response;
    }


    public static HttpResponse getMultiEntityInfo(String contractId) {
        HttpResponse multiEntityShowResponse = null;
        try {
            String apiMultiEntityPath = ContractShow.getMultiEntityAPIPath();

            apiMultiEntityPath = String.format(apiMultiEntityPath + contractId);

            HttpGet httpGet = new HttpGet(apiMultiEntityPath);

            httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");

            httpGet.addHeader("Accept", "application/json, text/plain, */*; q=0.01");

            httpGet.addHeader("scheme", "https");

            httpGet.addHeader("x-csrf-token", "072d15b7-c4f5-4a25-908b-936d6e58054c");

            multiEntityShowResponse = APIUtils.getRequest(httpGet);
        } catch (Exception e) {
            logger.error("Exception while hitting AutoExtraction MultiEntity API. {}", e.getMessage());
        }

        return multiEntityShowResponse;

    }

    public static HttpResponse getViewExtractedTextData(String cdrId) {
        HttpResponse multiEntityShowResponse = null;
        try {
            String apiMultiEntityPath = CDRShow.getMultiEntityAPIPath();

            apiMultiEntityPath = String.format(apiMultiEntityPath + cdrId);

            HttpGet httpGet = new HttpGet(apiMultiEntityPath);

            httpGet.addHeader("Content-Type", "application/json;charset=UTF-8");

            httpGet.addHeader("Accept", "application/json, text/plain, */*; q=0.01");

            httpGet.addHeader("scheme", "https");

            httpGet.addHeader("x-csrf-token", "072d15b7-c4f5-4a25-908b-936d6e58054c");

            multiEntityShowResponse = APIUtils.getRequest(httpGet);
        } catch (Exception e) {
            logger.error("Exception while hitting AutoExtraction MultiEntity API. {}", e.getMessage());
        }

        return multiEntityShowResponse;

    }

    public static HttpResponse checkMultiEntityDocListingMetadata(String query, String payload) {
        return checkAutoExtractionDocListingMetaData(query, payload);

    }

    //Calculating count of values received in JSON array for each indices
    public static int getCountOfValuesFromArray(JSONArray jsonResArr) {

        int valueCount = 0;
        //int sizeOfInnerArray = 0;
        try {
            int lengthOfJsonArr = jsonResArr.length();
            JSONObject jsoOfRes;
            for (int i = 0; i < lengthOfJsonArr; i++) {
                jsoOfRes = jsonResArr.getJSONObject(i);
                Set<String> eachKeySet = jsoOfRes.keySet();
                //sizeOfInnerArray = eachKeySet.size();
                for (String keySe : eachKeySet) {
                    String anotherResponseStr = jsoOfRes.get(keySe).toString();
                    JSONObject jsRes = new JSONObject(anotherResponseStr);
                    Set<String> newKeySet = jsRes.keySet();

                    for (String keyVa : newKeySet) {
                        String receiveTransformedTextField = jsRes.get("columnName").toString();
                        if (receiveTransformedTextField.equalsIgnoreCase("transformedtext")) {

                            valueCount = valueCount + 1;
                            break;

                        } else if (keyVa.equalsIgnoreCase("value")) {
                            String valueReceived = jsRes.get(keyVa).toString();
                            if (!valueReceived.isEmpty()) {
                                valueCount = valueCount + 1;
                                break;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching values from JSON response of AutoExtraction MultiEntity feature", e.getMessage());
        }
        return valueCount;

    }


    //Calculating count of each value repeated in Json array
    public static Map findTotalValueFromJsonArray(JSONArray jsonResArr, String columnName) {
        boolean isValueFound = false;
        Map<String, Integer> autoExtractionValueMap = new HashMap<String, Integer>();

        try {
            boolean isColumnFound = false;
            int lengthOfJsonArr = jsonResArr.length();
            JSONObject jsoOfRes;
            outerloop:
            for (int i = 0; i < lengthOfJsonArr; i++) {
                jsoOfRes = jsonResArr.getJSONObject(i);
                Set<String> eachKeySet = jsoOfRes.keySet();
                for (String keySe : eachKeySet) {
                    String anotherResponseStr = jsoOfRes.get(keySe).toString();
                    JSONObject jsRes = new JSONObject(anotherResponseStr);
                    Set<String> newKeySet = jsRes.keySet();
                    Iterator setValues = newKeySet.iterator();

                    while (setValues.hasNext()) {
                        String keyVa = setValues.next().toString();
                        if (keyVa.equalsIgnoreCase("columnName")) {
                            String columnNameReceived = jsRes.get(keyVa).toString();
                            if (columnNameReceived.equalsIgnoreCase(columnName)) {
                                isColumnFound = true;
                                String valueReceived = jsRes.get("value").toString();
                                //get the count of key
                                Integer countOfKey = autoExtractionValueMap.get(valueReceived);
                                if (countOfKey == null) {
                                    autoExtractionValueMap.put(valueReceived, 1);
                                } else {
                                    autoExtractionValueMap.put(valueReceived, countOfKey + 1);
                                }
                            }
                        }

                    }
                }
            }
        } catch (Exception e) {
            logger.error("Exception while fetching values from JSON response of AutoExtraction MultiEntity feature", e.getMessage());
        }

        return autoExtractionValueMap;

    }

    //Calculating inner size of Json array
    public static int getSizeOfInternalArray(JSONArray jsonResArr) {
        int sizeOfInnerArray = 0;
        try {
            JSONObject jsoOfRes = jsonResArr.getJSONObject(0);
            ;
            Set<String> eachKeySet = jsoOfRes.keySet();
            sizeOfInnerArray = eachKeySet.size();
        } catch (Exception e) {
            logger.error("Exception while fetching total size of JSON response of AutoExtraction MultiEntity feature", e.getMessage());
        }
        return sizeOfInnerArray;

    }

    public static HttpResponse autoExtractionMetaDataApi(String query, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete Delegation API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Metadata extraction API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getAllMetadataFields() {
        String query = "/metadataautoextraction/getAllFields";

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching API. {}", e.getMessage());
        }
        return response;
    }


    public static HttpResponse projectCreationAPI(String payload) {
        String query = "/metadataautoextraction/create";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse testProjectShowpage(int newlyCreatedProjectId) {
        String query = "/metadataautoextraction/show?projectId=" + newlyCreatedProjectId;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching created project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse projectUpdateAPI(String payload) {
        String query = "/metadataautoextraction/update";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getListDataForEntities(String entityId, String payload) {
        String query = "/listRenderer/list/" + entityId + "/listdata?version=2.0&isFirstCall=true";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("List Data API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting List Data API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse filterData(String listId, String payload) {
        String query = "/listRenderer/list/" + listId + "/filterData/";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/plain, */*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Automation Filter API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting automation filter Data API {}", e.getMessage());
        }
        return response;
    }

    public static JSONObject getJsonObjectForResponse(HttpResponse response) {
        JSONObject jsonObject = null;
        try {
            String responseString = EntityUtils.toString(response.getEntity());
            jsonObject = new JSONObject(responseString);
        } catch (Exception e) {
            logger.error("Error in converting http response to json error " + e.getStackTrace());
        }
        return jsonObject;
    }

    public static HttpResponse workflowAssignment(String payload) {
        String query = "/autoextraction/v1/moveEntity/workflow";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getUserId() {
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = "/options/3?vendorId=&relationId=&pageType=1&query=karuna";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get All Meta Data Fields API. {}", e.getMessage());
        }
        return response;
    }


    public static HttpResponse automationListing(String payload) {
        String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse deleteChart(String chartId) {
        String query = "/autoextraction/autoextractionCharts/delete/" + chartId + "";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Delete chart API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Delete Chart API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse createChart() {
        String query = "/autoextraction/dashboards/data/v1";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"selectAll\":true,\"bulkUpdateRequestData\":{\"listId\":432,\"filterMap\":{}},\"xaxisLevel\":[\"tags\"],\"yaxisLevel\":[\"count\"],\"xaxisDisplay\":\"DOCUMENT TAGS\",\"yaxisDisplay\":\"Count\",\"sqlQuery\":\"fetchDashBoardDataForTags\"}";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("create chart API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting create Chart API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse saveDashboard(String payload) {
        String query = "/autoextraction/saveDashboard";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("save chart API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting save Chart API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse stakeholderFilter(String payload) {
        String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getBulkEditData() {
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = "/autoextraction/v1/bulkEdit";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get All Meta Data Fields API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse qcLevel1TagCreation(String payload) {
        String query = "/metadataautoextraction/create/4";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse qcLevel2TagCreation(String payload) {
        String query = "/metadataautoextraction/create/5";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse docTagCreation(String payload) {
        String query = "/metadataautoextraction/create/3";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse assignedFilter() {
        String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"420\":{\"filterId\":\"420\",\"filterName\":\"unassigneddocs\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                    "\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"true\",\"name\":\"YesYes\"}]}}}},\"selectedColumns\":[{\"columnId\":16858,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":16430,\"columnQueryName\":\"documentname\"}," +
                    "{\"columnId\":16431,\"columnQueryName\":\"contracttype\"},{\"columnId\":16515,\"columnQueryName\":\"projects\"},{\"columnId\":16432,\"columnQueryName\":\"status\"},{\"columnId\":16874,\"columnQueryName\":\"metadatacount\"}," +
                    "{\"columnId\":16875,\"columnQueryName\":\"clausecount\"},{\"columnId\":17339,\"columnQueryName\":\"duplicatedata\"}]}";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse performBulkEdit(String payload) {
        String query = "/autoextraction/v1/performBulkEdit";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getMasterData() {
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = "/autoextraction/masterData";
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get All Meta Data Fields API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse unAssignedFilter() {
        String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"393\":{\"filterId\":\"393\",\"filterName\":\"metadatavalue\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}," +
                    "\"420\":{\"filterId\":\"420\",\"filterName\":\"unassigneddocs\",\"entityFieldId\":null,\"entityFieldHtmlType\":null,\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"0\"," +
                    "\"name\":\"UnassignedHide\"}]}},\"421\":{\"filterId\":\"421\",\"filterName\":\"folder\",\"entityFieldId\":null,\"entityFieldHtmlType\":null},\"448\":{\"filterId\":\"448\"," +
                    "\"filterName\":\"entityidsfilter\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}}},\"selectedColumns\":[{\"columnId\":16797,\"columnQueryName\":\"bulkcheckbox\"},{\"columnId\":17296," +
                    "\"columnQueryName\":\"id\"},{\"columnId\":16369,\"columnQueryName\":\"documentname\"},{\"columnId\":16454,\"columnQueryName\":\"projects\"},{\"columnId\":17345,\"columnQueryName\":\"batch\"}," +
                    "{\"columnId\":17289,\"columnQueryName\":\"doctags\"},{\"columnId\":17332,\"columnQueryName\":\"clusters\"},{\"columnId\":16371,\"columnQueryName\":\"status\"},{\"columnId\":257859,\"columnQueryName\":\"ManagerROLE_GROUP\"}," +
                    "{\"columnId\":17348,\"columnQueryName\":\"doctag1\"},{\"columnId\":17351,\"columnQueryName\":\"doctag2\"}]}";
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse projectListing() {
        String query = "/listRenderer/list/441/listdata?version=2.0&isFirstCall=true";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"filterMap\":{\"entityTypeId\":321,\"offset\":0,\"size\":20," +
                    "\"orderByColumnName\":\"documentcount\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{}},\"selectedColumns\":[]}";
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse createBatchSet(String payload) {
        String query = "/autoextraction/batchSet/createBatchSet";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse getBatchFilterData(String batchName) {
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = "/options/54?pageType=6&entityTpeId=316&pageEntityTypeId=316&query=" + batchName;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get All Meta Data Fields header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Get All Meta Data Fields API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse duplicateDataFilter() {
        String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            postRequest = new HttpPost(queryString);
            String payload = "\n" +
                    "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"393\":{\"filterId\":\"393\"," +
                    "\"filterName\":\"metadatavalue\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}," +
                    "\"421\":{\"filterId\":\"421\",\"filterName\":\"folder\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}," +
                    "\"438\":{\"filterId\":\"438\",\"filterName\":\"duplicatedocs\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                    "\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"AssignedShow\"}]}},\"448\":{\"filterId\":\"448\"," +
                    "\"filterName\":\"entityidsfilter\",\"entityFieldId\":null,\"entityFieldHtmlType\":null}}},\"selectedColumns\":[]}";
            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse triggerReference(int documentId) {
        String query = "/autoextraction/trigger/reference/documentId";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"documentId\":\"" + documentId + "\"}";
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse clauseTabListing(int documentId) {
        String query = "/listRenderer/list/493/listdata?isFirstCall=false";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":50," +
                    "\"orderByColumnName\":\"id\",\"orderDirection\":\"asc nulls first\"," +
                    "\"filterJson\":{\"366\":{\"multiselectValues\":{\"SELECTEDDATA\":[]},\"filterId\":366," +
                    "\"filterName\":\"categoryId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}}," +
                    "\"entityId\":\"" + documentId + "\"}";
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse projectLevelTrigger(int projectId) {
        String query = "/autoextraction/trigger/reference/projectid";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"projectId\":\"" + projectId + "\"}";
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse projectFilter(int projectId, String projectName) {
        String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20," +
                    "\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"385\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"" + projectId + "\"," +
                    "\"name\":\"" + projectName + "\"}]},\"filterId\":385,\"filterName\":\"projectids\"," +
                    "\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[]}";

            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());

        }
        return response;
    }


    public static HttpResponse docShowAPI(int recordId) {
        String query = "/autoextraction/v1/show/" + recordId;
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            getRequest.addHeader("Content-Type", "application/json;charset=UTF-8");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Show API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while validating Document show API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse qcLevelAllocation(int TagId, int Tag1Id, int Tag2Id, String documentIds) {
        String query = "/autoextraction/bulkUpdate/tag1Andtag2";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"randomDocuments\":{\"tag1\":[" + TagId + "],\"tag2\":[],\"size\":5}," +
                    "\"allDocuments\":{\"tag1\":[" + Tag1Id + "],\"tag2\":[" + Tag2Id + "]},\"documentIds\":[" + documentIds + "]}";
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());

        }
        return response;
    }

    public static HttpResponse tag2Creation(String tag2Name) {
        String query = "/metadataautoextraction/create/5";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"name\":\"" + tag2Name + "\"}";
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());

        }
        return response;
    }

    public static HttpResponse aeDocListing() {
        String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":10," +
                    "\"orderByColumnName\":\"datecreated\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{}},\"selectedColumns\":[]}";
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());

        }
        return response;
    }

    public static HttpResponse docTreeAPI(String recordId) {
        String query = "/autoextraction/documentHierarchicalView/" + recordId;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse fetchAllId(String payload) {
        String query = "/autoextraction/bulk/fetchAllIds/";

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Fetch All ID API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting fetch all Id API. {}", e.getMessage());

        }
        return response;
    }

    public static HttpResponse getLayoutAction(int recordId) {
        String query = "/v3/actions/316/" + recordId;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse parentReferenceFieldOptions(String recordId) {
        String query = "/options/53?pageType=1&entityTpeId=316&parentEntityId=&pageEntityTypeId=316&fieldId=&expandAutoComplete=true&offset=0&query=&expandAutoComplete=true&offset=0";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"autoExtractionCurrentId\":\"" + recordId + "\",\"autoExtractionChildIds\":[],\"autoExtractionParentIds\":[]}";
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());

        }
        return response;
    }

    public static HttpResponse batchSorting() {
        String query = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"batch\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":{}},\"selectedColumns\":[]}";
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Create Tag API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while Creating Project API. {}", e.getMessage());

        }
        return response;
    }

    public static HttpResponse checkAutoExtractionReportListingMetaData(String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = "/reportRenderer/list/520/defaultUserListMetaData";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Default user list Metadata API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while default user list metadata API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse blankFiltersForReport(String payload) {
        String query = "/reportRenderer/list/520/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "*/*");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("AE Tracker Report list  API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting AE Tracker Report List API. {}", e.getMessage());

        }
        return response;
    }

    public static HttpResponse downloadExcel(String documentId) {
        String query = "/autoextraction/api/downloadExcel?listIds=493%2C433&documentIds=" + documentId;
        ;
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            getRequest = new HttpGet(query);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get download Excel header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting download Excel  API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse viewHistoryAPI(String historyURL) {
        String query = historyURL;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);

            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse dnoListDataResponse(int listId, String payload) {
        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = "/listRenderer/list/" + listId + "/listdata?version=2.0&isFirstCall=true";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Default user list Metadata API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while default user list metadata API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse showCategory() {
        String query = "/metadataautoextraction/category/showCategory";
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Show Category API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching show category API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse showField() {
        String query = "/metadataautoextraction/field/showField";
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Show Field API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching show Field API. {}", e.getMessage());
        }
        return response;
    }
    public static HttpResponse extractionFeedback(String payload){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/autoExtraction/extractionFeedback";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");
            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Extraction feedback API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Extraction feedback API. {}", e.getMessage());
        }
        return response;
    }
    public static HttpResponse extractionFeedbackList(String payload,String listId){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/listRenderer/list/"+listId+"/listdata?version=2.0";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");
            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Extraction feedback API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Extraction feedback API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse updateClause(String payload){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/autoextraction/update/clauses";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");
            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Extraction update Clause API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Extraction Update Clause API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse referencesContractPorting(int contractId,int fieldid) {
        String query = "/clauseReference/field/61/"+contractId+"/"+fieldid+"";
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            logger.debug("Query string url formed is {}", query);
            getRequest = new HttpGet(query);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");

            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Get download Excel header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting download Excel  API. {}", e.getMessage());
        }
        return response;
    }
    public static HttpResponse extractionStatusComplete(){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/listRenderer/list/432/listdata?contractId=&relationId=&vendorId=&am=true&version=2.0";

            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\",\"orderDirection\":\"desc nulls last\"," +
                    "\"filterJson\":{\"368\":{\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"4\",\"name\":\"COMPLETED\"}]}," +
                    "\"filterId\":368,\"filterName\":\"statusId\",\"entityFieldHtmlType\":null,\"entityFieldId\":null}}},\"selectedColumns\":[]}";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Default user list Metadata API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while default user list metadata API. {}", e.getMessage());
        }
        return response;
    }


    public static HttpResponse metadataCreateOperation(String textId, String categoryId,int documentId, String fieldId){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/metadataautoextraction/modifyExtractedData";

            String payload = "{\"listId\":433,\"operation\":1,\"textId\":\""+textId+"\",\"categoryId\":\""+categoryId+"\"," +
                    "\"documentId\":\""+documentId+"\",\"data\":{\"fieldId\":"+fieldId+",\"previousValue\":[]," +
                    "\"updatedValue\":[\"API Automation Metadata Create Operation\"]}}";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Default user list Metadata API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while default user list metadata API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse metadataUpdateOperation(String textId, String fieldId){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/autoextraction/extractedData/actionType";

            String payload = "{\"extractedText\":\"API Automation Metadata Create Operation\",\"extractionTypeId\":1001," +
                    "\"textId\":\""+textId+"\",\"fieldId\":\""+fieldId+"\",\"actionType\":\"Update\"," +
                    "\"updatedText\":\"API Automation metadata Update Operation\"}";

            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Default user list Metadata API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while default user list metadata API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse metadataDeleteOperation(String textId, String fieldId){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/autoextraction/extractedData/actionType";

            String payload = "{\"extractedText\":\"API Automation metadata Update Operation\",\"extractionTypeId\":1001," +
                    "\"textId\":\""+textId+"\",\"fieldId\":\""+fieldId+"\",\"actionType\":\"Discard\",\"updatedText\":\"\"}";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Default user list Metadata API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while default user list metadata API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse clauseUpdateOperation(String clauseText, String textId, String clausePageNo, int documentId){

        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/autoextraction/update/clauses";

            String payload = "{\"text\":\""+clauseText+"\"," +
                    "\"pageNumber\":"+clausePageNo+",\"textId\":\""+textId+"\",\"documentId\":"+documentId+",\"categoryId\":1046,\"operation\":2," +
                    "\"aeCoordinate\":{\"X_Start\":335,\"Y_Start\":416,\"Height\":72,\"Width\":190,\"PageWidth\":595,\"PageHeight\":842,\"X_End\":525,\"Y_End\":488,\"PageNo\":5}}";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Default user list Metadata API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while default user list metadata API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse documentResetAPI(int documentId){

        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/autoextraction/redo";

            String payload = "{\"documentIds\":["+documentId+"]}";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Default user list Metadata API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while default user list metadata API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse showExtractionSummary(int contractId) {
        String query = "/autoextraction/contract/aeDocsStatusCount/"+contractId;
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Show Field API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching show Field API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse listDataAPILinkedWithContracts(int contractId){

        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/listRenderer/list/432/listdata?contractId="+contractId;

            String payload = "{\"filterMap\":{\"entityTypeId\":316,\"offset\":0,\"size\":20,\"orderByColumnName\":\"id\"," +
                    "\"orderDirection\":\"desc nulls last\",\"filterJson\":{\"438\":{\"filterId\":\"438\"," +
                    "\"filterName\":\"duplicatedocs\",\"entityFieldId\":null,\"entityFieldHtmlType\":null," +
                    "\"multiselectValues\":{\"SELECTEDDATA\":[{\"id\":\"1\",\"name\":\"AssignedShowExists\"}]}}}},\"selectedColumns\":[]}";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Default user list Metadata API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while default user list metadata API. {}", e.getMessage());
        }
        return response;
    }
    public static HttpResponse failedReset(String payload){
        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/autoextraction/redoFailedOnly";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);
            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");
            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Auto Extraction redo Failed Only API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Auto Re do Failed only API. {}", e.getMessage());
        }
        return response;
    }
    public static HttpResponse initiatePorting(int docId) {
        String query = "/autoextraction/portingApiDocIds?docIds=" + docId;
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Show Field API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching show Field API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse viewExtractedTextLink(int contractId) {
        String query = "/autoextraction/document/metadata?entityTypeId=61&entityId=" + contractId;
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Show Field API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching show Field API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse validateDocViewer(int docId) {
        String query = "/documentviewerstream/check/316/" +docId+ "/316/" +docId;
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Show Field API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching show Field API. {}", e.getMessage());
        }
        return response;
    }
    public static HttpResponse showDuplicateDocs(int contractId) {
        String query = "/autoextraction/contract/aeDuplicateDocs/"+contractId;
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Show Duplicate Docs API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting show duplicate Docs API. {}", e.getMessage());
        }
        return response;
    }


    public static HttpResponse commentSectionCDR(int cdrId){

        HttpResponse response = null;
        HttpPost postRequest;
        try{
            String queryString = "/listRenderer/list/65/tablistdata/160/"+cdrId;

            String payload = "{\"filterMap\":{\"entityTypeId\":null,\"offset\":0,\"size\":100,\"orderByColumnName\":\"id\",\"orderDirection\":\"asc\",\"filterJson\":{}}}";
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept","application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding","gzip, deflate");
            postRequest.addHeader("Content-Type","application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest,payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Default user list Metadata API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while default user list metadata API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse fetchFieldCategoryMappingsAPI(String fieldId)
    {
        String query = "/autoextraction/fetchFieldCategoryMappings/"+fieldId;

        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Show Duplicate Docs API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting show duplicate Docs API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse verifyDocumentOnTree(int contractId, int entityTypeId, int supplierId, String payload) {
        String query = "/contract-tree/v1/" +entityTypeId + "/" + contractId + "?hierarchy=false&offset=0&rootSupplierId="+supplierId;

        HttpResponse response = null;
        HttpPost postRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            postRequest = new HttpPost(queryString);

            postRequest.addHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            postRequest.addHeader("Accept-Encoding", "gzip, deflate");
            postRequest.addHeader("Content-Type", "application/json;charset=UTF-8");

            response = APIUtils.postRequest(postRequest, payload);
            logger.debug("Response status is {}", response.getStatusLine().toString());

            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Document On Tree API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while hitting Document On Tree API. {}", e.getMessage());
        }
        return response;
    }

    public static HttpResponse sideLayoutAPI() {
        String query = "/v1/sideLayout/data";
        HttpResponse response = null;
        HttpGet getRequest;
        try {
            String queryString = query;
            logger.debug("Query string url formed is {}", queryString);
            getRequest = new HttpGet(queryString);
            getRequest.addHeader("Accept", "application/json, text/plain, */*");
            getRequest.addHeader("Accept-Encoding", "gzip, deflate");
            response = APIUtils.getRequest(getRequest);
            logger.debug("Response status is {}", response.getStatusLine().toString());
            Header[] headers = response.getAllHeaders();
            for (Header oneHeader : headers) {
                logger.debug("Show Field API header {}", oneHeader.toString());
            }
        } catch (Exception e) {
            logger.error("Exception while fetching show Field API. {}", e.getMessage());
        }
        return response;
    }



}

