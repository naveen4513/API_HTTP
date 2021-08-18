package com.sirionlabs.api.clientAdmin;

import com.sirionlabs.api.commonAPI.Check;
import com.sirionlabs.helper.api.ApiHeaders;
import com.sirionlabs.helper.api.TestAPIBase;
import com.sirionlabs.helper.clientAdmin.AdminHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ClientShow extends TestAPIBase {

    private final static Logger logger = LoggerFactory.getLogger(ClientShow.class);

    public static String getApiPath(int clientId) {
        return "/tblclients/show/" + clientId;
    }

    public static HashMap<String, String> getHeaders() {
        return ApiHeaders.getDefaultHeadersForClientAdminAPIs();
    }

    public static String getClientShowResponse(int clientId) {
        String lastLoggedInUserName = Check.lastLoggedInUserName;
        String lastLoggedInUserPassword = Check.lastLoggedInUserPassword;

        AdminHelper adminHelperObj = new AdminHelper();
        adminHelperObj.loginWithClientAdminUser();

        logger.info("Hitting Show API Response for Client Id {}", clientId);
        String response = executor.get(getApiPath(clientId), getHeaders()).getResponse().getResponseBody();

        Check checkObj = new Check();
        checkObj.hitCheck(lastLoggedInUserName, lastLoggedInUserPassword);

        return response;
    }

    public static String getClientName(String clientShowResponse) {
        try {
            Document html = Jsoup.parse(clientShowResponse);
            return html.getElementsByClass("form-container").get(0).child(0).child(0).child(1).childNode(0).toString();

        } catch (Exception e) {
            logger.error("Exception while Getting Client Name from Client Show Response. " + e.getMessage());
        }

        return null;
    }

    public static List<String> getAllFunctions(String clientShowResponse) {
        List<String> allFunctions = new ArrayList<>();

        try {
            Document html = Jsoup.parse(clientShowResponse);
            Elements functionalDivs = html.getElementById("l_com_sirionlabs_model_functionalHierarchy").children().get(1).children();

            for (int i = 0; i < functionalDivs.size() - 1; i++) {
                String functionName = functionalDivs.get(i).child(0).child(0).childNode(0).toString().trim();
                allFunctions.add(functionName);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Functions. {}", e.getMessage());
            return null;
        }

        return allFunctions;
    }

    public static List<String> getAllServices(String clientShowResponse) {
        List<String> allServices = new ArrayList<>();

        try {
            Document html = Jsoup.parse(clientShowResponse);
            Elements functionalDivs = html.getElementById("l_com_sirionlabs_model_functionalHierarchy").children().get(1).children();

            for (int i = 0; i < functionalDivs.size() - 1; i++) {
                String[] servicesArr = functionalDivs.get(i).child(1).child(0).childNode(0).toString().trim().split(",");

                for (String serviceName : servicesArr) {
                    serviceName = serviceName.trim();

                    if (serviceName.contains("&amp;")) {
                        serviceName = serviceName.replace("&amp;", "&");
                    }

                    allServices.add(serviceName);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Services. {}", e.getMessage());
            return null;
        }

        return allServices;
    }

    public static List<String> getAllRegions(String clientShowResponse) {
        List<String> allRegions = new ArrayList<>();

        try {
            Document html = Jsoup.parse(clientShowResponse);
            Elements regionalDivs = html.getElementById("l_com_sirionlabs_model_regionalHierarchy").children().get(1).children();

            for (int i = 0; i < regionalDivs.size() - 1; i++) {
                String regionName = regionalDivs.get(i).child(0).child(0).childNode(0).toString().trim();
                allRegions.add(regionName);
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Regions. {}", e.getMessage());
            return null;
        }

        return allRegions;
    }

    public static List<String> getAllCountries(String clientShowResponse) {
        List<String> allCountries = new ArrayList<>();

        try {
            Document html = Jsoup.parse(clientShowResponse);
            Elements regionalDivs = html.getElementById("l_com_sirionlabs_model_regionalHierarchy").children().get(1).children();

            for (int i = 0; i < regionalDivs.size() - 1; i++) {
                String[] countriesArr = regionalDivs.get(i).child(1).child(0).childNode(0).toString().trim().split(",");

                for (String countryName : countriesArr) {
                    countryName = countryName.trim();

                    if (countryName.contains("&amp;")) {
                        countryName = countryName.replace("&amp;", "&");
                    }

                    allCountries.add(countryName);
                }
            }
        } catch (Exception e) {
            logger.error("Exception while Getting All Countries. {}", e.getMessage());
            return null;
        }

        return allCountries;
    }


    public static List<String> getAllCurrencies(String clientShowResponse) {
        List<String> allCurrencies = new ArrayList<>();

        try {
            Document html = Jsoup.parse(clientShowResponse);
            Elements divs = html.getElementById("_c_com_sirionlabs_model_clientcurrencies_clientCurrencyList_id").children().get(0).children();

            allCurrencies = Arrays.asList((divs.get(0).child(0).child(0).childNode(0).toString().split("\n")));


        } catch (Exception e) {
            logger.error("Exception while Getting All Regions. {}", e.getMessage());
            return null;
        }

        return allCurrencies;
    }
}