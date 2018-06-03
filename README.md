# adobeio-authentication-java-sample

Simple code that demonstrates how to authenticate against adobe.io using Java

## How to use this code

There is some setup that you have to do.

1. Create a "secret.key" file based on the private/public key you made for the Adobe.io integration
2. Copy values from the Adobe.io Console
3. Copy a property ID from the Launch UI
4. Decide whether you want to spy on traffic using Charles or another proxy
5. See notes on Debugging further down
6. Run

## Debugging

You can use Charles (or another proxy) to debug traffic between the app and Adobe IO.

Make sure you set the `PROXY_HOST` and `PROXY_PORT` constants correctly. The actual choice of using a proxy or not happens when you create the `httpClient` on line 75 of the `AppTest` class. Just use the HttpClientBuilder of your choice.

## Notes

I am using the Launch API to check whether my access works.

If you want to use any other API, you need to change `metaContexts`, `apiHostFQDN`, and `apiEndpoint`, accordingly.

Code quality: this is the most appalling spaghetti-code I have written in a long time. This was deliberate. I want to show exactly which steps need to be taken in what order.

Feel free to refactor to your standards. 