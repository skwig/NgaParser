package sk.brecka;

import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

public class Main {

    // https://images.nga.gov/en/search/show_advanced_search_page/?service=search&action=do_advanced_search&language=en&form_name=default&all_words=&exact_phrase=&exclude_words=&artist_last_name=&keywords_in_title=&accession_number=&school=&Classification=&medium=&year=&year2=&open_access=Open+Access+Available

    static int totalCounter = 0;

    public static final int PAGE_CAUGHT_PERIOD = 10_000;
    public static final int PAGE_STARTING_PERIOD = 1_000;


    public static final int JSON_CAUGHT_PERIOD = 5_000;
    public static final int JSON_STARTING_PERIOD = 20;

    public static final int DOWNLOAD_CAUGHT_PERIOD = 5_000;
    public static final int DOWNLOAD_STARTING_PERIOD = 500;

    private static String transformKey(String key) {
        return key.toLowerCase()
                .replace(' ', '_')
                .replace(".", "");
    }

    private static void downloadAsset(int assetId, int sizeId) throws IOException {

        final String json = "{\"mainForm\":{\"project_title\":\"\",\"usage\":\"-1\"},\"assets\":{\"a0\":{\"assetId\":\"" + assetId + "\",\"sizeId\":\"" + sizeId + "\"}}}";
        final String downloadFolder = "/media/storage/downloads/nga/";

        Base64.Encoder encoder = Base64.getEncoder();
        String urlEncoded = URLEncoder.encode(json, "UTF-8");
        String base64Encoded = encoder.encodeToString(urlEncoded.getBytes());

        URL website = new URL("https://images.nga.gov/?service=basket" +
                "&action=do_direct_download" +
                "&type=dam" +
                "&data=" + base64Encoded);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(downloadFolder + String.valueOf(assetId) + ".zip");
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

    }

    private static void buildAssetDatabase() throws FileNotFoundException {
        List<Thread> threadList = new ArrayList<>();

        final int sizeId = 1;

        Scanner scanner = new Scanner(new File("assetid_database"));
        int counter = 0;
        while (scanner.hasNextLine()) {
            counter++;
            final int assetId = Integer.parseInt(scanner.nextLine());

            System.out.println("Started: " + counter);

            Thread thread = new Thread(() -> {
                boolean successful = false;

                while (!successful) {

                    try {

                        downloadAsset(assetId, sizeId);

                        totalCounter++;

                        if (totalCounter % 50 == 0) {
                            System.out.println("### Finished: " + totalCounter);
                        }

                        successful = true;

                    } catch (IOException e) {
                        System.out.println("@@@ Failed " + assetId);
                        try {
                            Thread.sleep(DOWNLOAD_CAUGHT_PERIOD);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });

            threadList.add(thread);

            thread.start();

            try {
                Thread.sleep(DOWNLOAD_STARTING_PERIOD);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        System.out.println("Vsetky thready vytvorene");

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Koniec");
    }

    private static void buildAssetIdDatabase() {
        final int pageCount = 2071;

        List<Thread> threadList = new ArrayList<>();

        for (int i = 1; i <= pageCount; i++) {

            System.out.println("Started: " + i + "/" + pageCount);

            final int page = i;

            Thread thread = new Thread(() -> {
                boolean successful = false;

                while (!successful) {
                    try (PrintWriter printWriter = new PrintWriter("pages/" + page)) {

                        String pageUrl = "https://images.nga.gov/en/search/do_advanced_search.html" +
                                "?form_name=default" +
                                "&all_words=" +
                                "&exact_phrase=" +
                                "&exclude_words=" +
                                "&artist_last_name=" +
                                "&keywords_in_title=" +
                                "&accession_number=" +
                                "&school=" +
                                "&Classification=" +
                                "&medium=" +
                                "&year=" +
                                "&year2=" +
                                "&open_access=Open%20Access%20Available" +
                                "&q=" +
                                "&mime_type=" +
                                "&qw=" +
                                "&page=" + page +
                                "&grid_layout=3";

                        Document pageDocument = Jsoup.connect(pageUrl).get();

                        Elements elements = pageDocument.select("img[assetid]");

                        for (Element element : elements) {
                            printWriter.println(element.attr("assetid"));
                        }

                        totalCounter++;

                        if (totalCounter % 50 == 0) {
                            System.out.println("### Finished: " + totalCounter + "/" + pageCount + " ###");
                        }
                        successful = true;

                    } catch (IOException e) {
                        System.out.println("@@@ Failed " + page + " @@@");
                        try {
                            Thread.sleep(PAGE_CAUGHT_PERIOD);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });

            threadList.add(thread);

            thread.start();

            try {
                Thread.sleep(PAGE_STARTING_PERIOD);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        System.out.println("Vsetky thready vytvorene");

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Koniec");
    }

    private static void buildAssetJ́sonDatabase() throws FileNotFoundException {
        List<Thread> threadList = new ArrayList<>();


        Scanner scanner = new Scanner(new File("assetid_database"));
        int counter = 0;
        while (scanner.hasNextLine()) {
            counter++;
            final int assetId = Integer.parseInt(scanner.nextLine());

            System.out.println("Started: " + counter);

            Thread thread = new Thread(() -> {
                boolean successful = false;

                while (!successful) {

                    try (PrintWriter printWriter = new PrintWriter("jsons/" + assetId + ".json")) {
                        String paintingUrl = "https://images.nga.gov/" +
                                "?service=asset" +
                                "&action=show_zoom_window_popup" +
                                "&language=en" +
                                "&asset=" + assetId;

                        Document paintingDocument = Jsoup.connect(paintingUrl).get();

                        Elements dts = paintingDocument.select("#info > dl").select("dt");
                        Elements dds = paintingDocument.select("#info > dl").select("dd");

                        if (dts.size() == 0 || dds.size() == 0) {
                            System.out.println("&&& Empty" + assetId);

                            try {
                                Thread.sleep(JSON_CAUGHT_PERIOD);
                            } catch (InterruptedException e1) {
                                e1.printStackTrace();
                            }

                            continue;
                        }

                        assert dts.size() == dds.size();

                        JSONObject paintingObject = new JSONObject();

                        for (int i = 0; i < dts.size(); i++) {
                            paintingObject.put(transformKey(dts.get(i).text()), dds.get(i).text());
                        }

                        printWriter.print(paintingObject.toString());

                        totalCounter++;

                        if (totalCounter % 50 == 0) {
                            System.out.println("### Finished: " + totalCounter);
                        }

                        successful = true;

                    } catch (IOException e) {
                        System.out.println("@@@ Failed " + assetId);
                        try {
                            Thread.sleep(JSON_CAUGHT_PERIOD);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }
                }
            });

            threadList.add(thread);

            thread.start();

            try {
                Thread.sleep(JSON_STARTING_PERIOD);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        System.out.println("Vsetky thready vytvorene");

        for (Thread thread : threadList) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Koniec");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        // write your code here

//        buildAssetIdDatabase();
//        buildAssetJsonDatabase();
        buildAssetDatabase();


//
//        // {"mainForm":{"project_title":"","usage":"-1"},"assets":{"a0":{"assetId":"18410","sizeId":"3"}}}
//        String json = "{\"mainForm\":{\"project_title\":\"\",\"usage\":\"-1\"},\"assets\":{\"a0\":{\"assetId\":\"18410\",\"sizeId\":\"3\"}}}";

        // 2071 stran

        // 25


        //
//        String paintingPageUrl = "https://images.nga.gov/en/search/show_advanced_search_page/" +
//                "?service=search" +
//                "&action=do_advanced_search" +
//                "&language=en" +
//                "&form_name=default" +
//                "&all_words=" +
//                "&exact_phrase=" +
//                "&exclude_words=" +
//                "&artist_last_name=" +
//                "&keywords_in_title=" +
//                "&accession_number=" +
//                "&school=" +
//                "&Classification=" +
//                "&medium=" +
//                "&year=" +
//                "&year2=" +
//                "&open_access=Open+Access+Available";
//
//        "https://images.nga.gov/en/search/show_advanced_search_page/" +
//                "?service=search" +
//                "&action=do_advanced_search" +
//                "&language=en" +
//                "&form_name=default" +
//                "&all_words=" +
//                "&exact_phrase=" +
//                "&exclude_words=" +
//                "&artist_last_name=" +
//                "&keywords_in_title=" +
//                "&accession_number=" +
//                "&school=" +
//                "&Classification=" +
//                "&medium=" +
//                "&year=" +
//                "&year2=" +
//                "&open_access=Open+Access+Available"
//
//
//        String paintingUrl = "https://images.nga.gov/" +
//                "?service=asset" +
//                "&action=show_zoom_window_popup" +
//                "&language=en" +
//                "&asset=18435";
//
//        Document paintingDocument = Jsoup.connect(paintingUrl).get();
//
//        Elements dts = paintingDocument.select("#info > dl").select("dt");
//        Elements dds = paintingDocument.select("#info > dl").select("dd");
//
//        assert dts.size() == dds.size();
//
//        JSONObject paintingObject = new JSONObject();
//
//        for (int i = 0; i < dts.size(); i++) {
//            paintingObject.put(transformKey(dts.get(i).text()), dds.get(i).text());
//        }
//
//
//        System.out.println(paintingObject);

//        JSONObject jsonObject = new JSONObject(json);
//
//        System.out.println(jsonObject);
//
//        Base64.Encoder encoder = Base64.getEncoder();
//        String urlEncoded = URLEncoder.encode(json, "UTF-8");
//        String base64Encoded = encoder.encodeToString(urlEncoded.getBytes());
//
//        System.out.println(json);
//        System.out.println(urlEncoded);
//        System.out.println(base64Encoded);

        //
//        String out = encoder.encodeToString(encoder.encode(json.getBytes(Charset.forName("UTF-8"))));
//
//        System.out.println(new String(Base64.getUrlDecoder().decode(out)));
//
//        System.out.println(out);
//
//        URL website = new URL("https://images.nga.gov/?service=basket" +
//                "&action=do_direct_download" +
//                "&type=dam" +
//                "&data=JTdCJTIybWFpbkZvcm0lMjIlM0ElN0IlMjJwcm9qZWN0X3RpdGxlJTIyJTNBJTIyJTIyJTJDJTIydXNhZ2UlMjIlM0ElMjItMSUyMiU3RCUyQyUyMmFzc2V0cyUyMiUzQSU3QiUyMmEwJTIyJTNBJTdCJTIyYXNzZXRJZCUyMiUzQSUyMjE4NDEwJTIyJTJDJTIyc2l6ZUlkJTIyJTNBJTIyMyUyMiU3RCU3RCU3RA==");
//        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
//        FileOutputStream fos = new FileOutputStream("foo");
//        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);

        // json -> url encode -> base64 encode -> url get param
        // stiahne zip v ktorom je obrazok

//        https://images.nga.gov/?service=basket&action=do_direct_download&type=dam&data=JTdCJTIybWFpbkZvcm0lMjIlM0ElN0IlMjJwcm9qZWN0X3RpdGxlJTIyJTNBJTIyJTIyJTJDJTIydXNhZ2UlMjIlM0ElMjItMSUyMiU3RCUyQyUyMmFzc2V0cyUyMiUzQSU3QiUyMmEwJTIyJTNBJTdCJTIyYXNzZXRJZCUyMiUzQSUyMjE4NDEwJTIyJTJDJTIyc2l6ZUlkJTIyJTNBJTIyMyUyMiU3RCU3RCU3RA==
    }
}
