# Akka Response Entity Subscription Timeout

## What is this repository about?
This project was created because I observed some things I didn't understand when using Akka HTTP and Akka Streams.
I'm not sure if what I observed is a bug or intended behavior, so I decided to provide a couple of small, standalone examples that demonstrate my observations.

## What are you actually trying to achieve?
All examples have the same context: Consuming Multipart Documents from a paginated HTTP resource and stream the contents.
The resource might serve similar purposes to those of an RSS-Feed.  
Each page can be obtained with a GET request. If the page has a predecessor and/or a successor, the response contains `next` and `prev` headers that contain links pointing to the corresponding resources.

A consumer of this resource can then start at any given page, consume the page's content by taking the response body, splitting it into its parts and then processing each part individually.
It can then use the `next` header to fetch the next page, process the content, and then continue crawling the pages one by one.

I think this is a perfect fit for a streaming solution:
* The different stages of the stream (doing the HTTP requests, splitting the Multipart document, processing the individual parts) might have different characteristics: memory consumption, computation power, ...
* Especially for large documents it might make sense to process the data in chunks in order to avoid keeping the whole response in memory (less memory consumption)
* If one of the stages is slower (e.g. the last part is very slow, because it is persisting each element and the DB is slow), it could backpressure and the upstream could act accordingly
* It might make sense to put buffers in between some of the stages to maximize overall throughput
* The different stages could be separated by asynchronous boundaries

## Sounds great, where's the problem?
### The initial implementation
The first implementation was based on these steps:

1. Crawl through the pages, do the HTTP requests and provide a `Source[HttpResponse]`
2. The `HttpResponse`s can then be unmarshalled: `Source[Multipart.General]`
3. The Multipart documents can then be flattened into the individual parts: `Source[Multipart.General.BodyPart]` 

This source can then be used to process the parts.

For the first step, I used the approach described in [this answer on stackoverflow](https://stackoverflow.com/a/39458276/5247502).
It uses `Source.unfoldAsync` to create the `Source[HttpResponse]`.
It encapsulates the fact that the crawling requires a cyclic dependency:
The response of a request (the response's headers, that is) is necessary to create the next request if there is one.
Since the `HTTP.singleRequest()` returns a `Future[HttpResponse]`, we need to use `Source.unfoldAsync` instead of `Source.unfold`.  
The other steps can be implemented in a straightforward way using the built in operators of Akka Streams.

You can find an implementation of this idea in [UnfoldAsyncTest](src/main/scala/com/l7r7/lab/client/UnfoldAsyncTest.scala).
To keep things simple it doesn't crawl through pages but instead repeatedly requests the same page.
This creates an infinite source of pages which is perfectly fine for this demonstration.

### The problem with the first implementation
Before you run the example, make sure to start the server [as described below](#how-can-i-try-it).
Then run

    sbt "runMain com.l7r7.lab.client.UnfoldAsyncTest"

The code will consume 1000 pages, split each into its parts and print the Content-Type for each part along with an index.
After a couple of entries (how many exactly may depend on your hardware), you will see this log line:

> ```[WARN] [04/20/2019 20:59:58.594] [default-akka.actor.default-dispatcher-2] [default/Pool(shared->http://localhost:8080)] [0 (WaitingForResponseEntitySubscription)] Response entity was not subscribed after 1 second. Make sure to read the response entity body or call `discardBytes()` on it. GET /content Empty -> 200 OK Default(165550 bytes)```

I'm not exactly sure what's going on here, but it causes the whole stream to stop working.
It doesn't break it, but it stalls and doesn't make any further progress.
After searching a little bit, I found [this page in Akka's documentation](https://doc.akka.io/docs/akka-http/current/implications-of-streaming-http-entity.html), which says that it is important to consume or discard the response entity (which absolutely makes sense).
And as far as I understand, the warning appears because exactly this doesn't happen.  
Of course it's possible to change the timeout by setting `akka.http.host-connection-pool.response-entity-subscription-timeout` to a value other than 1 second.
But I think this is not the right thing to do because it doesn't solve the real problem and it might also stall the host connection pool if too may connections are open for a long time waiting for subscription.

I also found [an issue](https://github.com/akka/akka-http/issues/1836) in Akka HTTP where a similar problem is described.
The discussion on the issue contains some valuable bits of information:
* The relatively short value for the subscription timeout is a safeguard that makes it obvious when one forgets to subscribe to a response.
* The subscription timeout could be set to some arbitrary high number to work around the issue. However, as mentioned above, this might lead to an exhausted connection pool because all connections are used. In addition to that, the server might close the conneciton if it takes too long to process the results.
* Apparently it is not a good idea to mix Akka HTTP and Akka Streams. Instead, it's better to do all the HTTP related stuff in one go and then switch to streaming things. [There's an open pull request](https://github.com/akka/akka-http/pull/2270/) related to the issue which updates the documentation to suggest exactly that.

### The pain points

I'm a bit confused by the fact that Akka Streams and Akka HTTP don't work together as smoothly as expected.
The docs of Akka HTTP at several points suggest using both Streams and HTTP closely together (one example is the [Flow-based variant of the Host Connection Pool](https://doc.akka.io/docs/akka-http/current/client-side/request-level.html#flow-based-variant)).
My experience (and I'm obviously not the only one here, as the issue linked above shows) tells me not to do so.

So the biggest questions to me are:
* Is this intended behavior? 
* Is it a bug somewhere in Akka?
* Am I using it the wrong way?

## I have an idea for how you can solve this problem!

Great! I'm more than happy to hear about what you think.
If you want to contact me, open up an issue in this repo, or [hit me up on Twitter](https://twitter.com/l7r7_).
I'm looking forward to any kind of constructive discussion

## How can I try it?

The code contains a little server that serves a single, static Multipart document.
You can start the server by running

    sbt "runMain com.l7r7.lab.server.FeedServer"

Then start one of the clients. With

    sbt run
    
you will get a list of all objects that can be run 
