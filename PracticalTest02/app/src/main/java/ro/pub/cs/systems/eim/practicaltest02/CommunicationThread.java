package ro.pub.cs.systems.eim.practicaltest02;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.util.EntityUtils;

public class CommunicationThread extends Thread {
    private ServerThread serverThread;
    private Socket socket;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    @Override
    public void run() {
        if (socket == null) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
            return;
        }
        try {
            BufferedReader bufferedReader = Utilities.getReader(socket);
            PrintWriter printWriter = Utilities.getWriter(socket);
            if (bufferedReader == null || printWriter == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Buffered Reader / Print Writer are null!");
                return;
            }
            Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client (city / information type!");
            String currencyType = bufferedReader.readLine();
            if (currencyType == null || currencyType.isEmpty()) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error receiving parameters from client (city / information type!");
                return;
            }
            HashMap<String, ValueClass> data = serverThread.getData();
            ValueClass valueClass = null;
            if (data.containsKey(currencyType) && !data.get(currencyType).date.before(new Date(1 * 1000 * 60))) {
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the cache...");
                Log.d(Constants.TAG, "Cache");
                valueClass = data.get(currencyType);
            } else {
                Log.d(Constants.TAG, "Http Request");
                Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
                HttpClient httpClient = new DefaultHttpClient();
                String pageSourceCode = "";
                if(false) {
                    /*HttpPost httpPost = new HttpPost(Constants.WEB_SERVICE_ADDRESS);
                    List<NameValuePair> params = new ArrayList<>();
                    params.add(new BasicNameValuePair("q", city));
                    params.add(new BasicNameValuePair("mode", Constants.WEB_SERVICE_MODE));
                    params.add(new BasicNameValuePair("APPID", Constants.WEB_SERVICE_API_KEY));
                    UrlEncodedFormEntity urlEncodedFormEntity = new UrlEncodedFormEntity(params, HTTP.UTF_8);
                    httpPost.setEntity(urlEncodedFormEntity);
                    ResponseHandler<String> responseHandler = new BasicResponseHandler();

                    pageSourceCode = httpClient.execute(httpPost, responseHandler);*/
                } else {
                    HttpGet httpGet = null;
                    if (currencyType.compareTo("USD") == 0) {
                        httpGet = new HttpGet(Constants.USD_URL);
                    }
                    if (currencyType.compareTo("EUR") == 0) {
                        Log.d(Constants.TAG, currencyType);
                        httpGet = new HttpGet(Constants.EUR_URL);
                        Log.d(Constants.TAG, httpGet.toString());
                    }
                    Log.d(Constants.TAG, currencyType);
                    Log.d(Constants.TAG, "a mers call ul");
                    HttpResponse httpGetResponse = null;
                    try {
                        httpGetResponse = httpClient.execute(httpGet);
                    }
                    catch (Exception ex)
                    {
                        Log.d(Constants.TAG, ex.toString());
                    }
                    Log.d(Constants.TAG, "a mers call ul");
                    HttpEntity httpGetEntity = httpGetResponse.getEntity();
                    Log.d(Constants.TAG, "a mers call ul");
                    if (httpGetEntity != null) {
                        pageSourceCode = EntityUtils.toString(httpGetEntity);
                    }
                }

                if (pageSourceCode == null) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                    return;
                } else
                    Log.i(Constants.TAG, pageSourceCode );

                // Updated for openweather API
                if (false) {
                    /*Document document = Jsoup.parse(pageSourceCode);
                    Element element = document.child(0);
                    Elements elements = element.getElementsByTag(Constants.SCRIPT_TAG);
                    for (Element script : elements) {
                        String scriptData = script.data();
                        if (scriptData.contains(Constants.SEARCH_KEY)) {
                            int position = scriptData.indexOf(Constants.SEARCH_KEY) + Constants.SEARCH_KEY.length();
                            scriptData = scriptData.substring(position);
                            JSONObject content = new JSONObject(scriptData);
                            JSONObject currentObservation = content.getJSONObject(Constants.CURRENT_OBSERVATION);
                            String temperature = currentObservation.getString(Constants.TEMPERATURE);
                            String windSpeed = currentObservation.getString(Constants.WIND_SPEED);
                            String condition = currentObservation.getString(Constants.CONDITION);
                            String pressure = currentObservation.getString(Constants.PRESSURE);
                            String humidity = currentObservation.getString(Constants.HUMIDITY);
                            weatherForecastInformation = new WeatherForecastInformation(
                                    temperature, windSpeed, condition, pressure, humidity
                            );
                            serverThread.setData(city, weatherForecastInformation);
                            break;
                        }
                    }*/
                } else {
                    JSONObject content = new JSONObject(pageSourceCode);
                    Log.d(Constants.TAG, "ceva");
                    JSONObject bpi = content.getJSONObject("bpi");
                    Log.d(Constants.TAG, "ceva");
                    JSONObject value = null;
                    if (currencyType.compareTo("USD") == 0)
                        value = bpi.getJSONObject("USD");
                    if (currencyType.compareTo("EUR") == 0)
                        value = bpi.getJSONObject("EUR");
                    Log.d(Constants.TAG, "ceva");
                    String rate = value.getString("rate");
                    Log.d(Constants.TAG, "ceva");
                    valueClass = new ValueClass(rate);
                    serverThread.setData(currencyType, valueClass);
                }
            }
            if (valueClass == null) {
                Log.e(Constants.TAG, "[COMMUNICATION THREAD] valueClass is null!");
                return;
            }
            String result = valueClass.toString();
            printWriter.println(result);
            printWriter.flush();
        } catch (IOException ioException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
            if (Constants.DEBUG) {
                ioException.printStackTrace();
            }
        } catch (JSONException jsonException) {
            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + jsonException.getMessage());
            if (Constants.DEBUG) {
                jsonException.printStackTrace();
            }
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ioException) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                    if (Constants.DEBUG) {
                        ioException.printStackTrace();
                    }
                }
            }
        }
    }
}
