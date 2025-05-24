/*
 * Copyright (C) 2025 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.stario.launcher.sheet.briefing.rss;

import static com.apptasticsoftware.rssreader.util.Mapper.createIfNull;
import static com.apptasticsoftware.rssreader.util.Mapper.createIfNullOptional;
import static com.apptasticsoftware.rssreader.util.Mapper.mapInteger;
import static com.apptasticsoftware.rssreader.util.Mapper.mapLong;
import static javax.xml.stream.XMLStreamConstants.CDATA;
import static javax.xml.stream.XMLStreamConstants.CHARACTERS;
import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import android.util.Log;

import androidx.annotation.NonNull;

import com.apptasticsoftware.rssreader.Channel;
import com.apptasticsoftware.rssreader.DateTime;
import com.apptasticsoftware.rssreader.DateTimeParser;
import com.apptasticsoftware.rssreader.Enclosure;
import com.apptasticsoftware.rssreader.Image;
import com.apptasticsoftware.rssreader.Item;
import com.apptasticsoftware.rssreader.util.Mapper;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.SSLContext;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// 26 Jul '24 Razvan Albu
// Migrated to woodstox and geronimo StAX to run on Android.

/**
 * Abstract base class for implementing modules or extensions of RSS / Atom feeds with custom tags and attributes.
 */
public abstract class WoodstoxAbstractRssReader<C extends Channel, I extends Item> {
    private static final String LOG_GROUP = "com.apptasticsoftware.rssreader";
    private final OkHttpClient httpClient;
    private DateTimeParser dateTimeParser = new DateTime();
    private final Map<String, String> headers = new HashMap<>();
    private final HashMap<String, BiConsumer<C, String>> channelTags = new HashMap<>();
    private final HashMap<String, Map<String, BiConsumer<C, String>>> channelAttributes = new HashMap<>();
    private final HashMap<String, Consumer<I>> onItemTags = new HashMap<>();
    private final HashMap<String, BiConsumer<I, String>> itemTags = new HashMap<>();
    private final HashMap<String, Map<String, BiConsumer<I, String>>> itemAttributes = new HashMap<>();
    private final Set<String> collectChildNodesForTag = Set.of("content", "summary");
    private boolean isInitialized;


    /**
     * Constructor
     */
    protected WoodstoxAbstractRssReader() {
        httpClient = createHttpClient();
    }

    /**
     * Constructor
     *
     * @param httpClient http client
     */
    protected WoodstoxAbstractRssReader(OkHttpClient httpClient) {
        Objects.requireNonNull(httpClient, "Http client must not be null");
        this.httpClient = httpClient;
    }

    /**
     * Returns an object of a Channel implementation.
     *
     * @return channel
     * @deprecated Use {@link WoodstoxAbstractRssReader#createChannel(DateTimeParser)} instead.
     */
    @SuppressWarnings("java:S1133")
    @Deprecated(since = "3.5.0", forRemoval = true)
    protected C createChannel() {
        return null;
    }

    /**
     * Returns an object of a Channel implementation.
     *
     * @param dateTimeParser dateTimeParser
     * @return channel
     */
    protected abstract C createChannel(DateTimeParser dateTimeParser);

    /**
     * Returns an object of an Item implementation.
     *
     * @return item
     * @deprecated Use {@link WoodstoxAbstractRssReader#createItem(DateTimeParser)} instead.
     */
    @SuppressWarnings("java:S1133")
    @Deprecated(since = "3.5.0", forRemoval = true)
    protected I createItem() {
        return null;
    }

    /**
     * Returns an object of an Item implementation.
     *
     * @param dateTimeParser dateTimeParser
     * @return item
     */
    protected abstract I createItem(DateTimeParser dateTimeParser);

    /**
     * Initialize channel and items tags and attributes
     */
    protected void initialize() {
        registerChannelTags();
        registerChannelAttributes();
        registerItemTags();
        registerItemAttributes();
    }

    /**
     * Register channel tags for mapping to channel object fields
     */
    @SuppressWarnings("java:S1192")
    protected void registerChannelTags() {
        channelTags.putIfAbsent("title", Channel::setTitle);
        channelTags.putIfAbsent("description", Channel::setDescription);
        channelTags.putIfAbsent("subtitle", Channel::setDescription);
        channelTags.putIfAbsent("link", Channel::setLink);
        channelTags.putIfAbsent("category", Channel::addCategory);
        channelTags.putIfAbsent("language", Channel::setLanguage);
        channelTags.putIfAbsent("copyright", Channel::setCopyright);
        channelTags.putIfAbsent("rights", Channel::setCopyright);
        channelTags.putIfAbsent("generator", Channel::setGenerator);
        channelTags.putIfAbsent("ttl", Channel::setTtl);
        channelTags.putIfAbsent("pubDate", Channel::setPubDate);
        channelTags.putIfAbsent("lastBuildDate", Channel::setLastBuildDate);
        channelTags.putIfAbsent("updated", Channel::setLastBuildDate);
        channelTags.putIfAbsent("managingEditor", Channel::setManagingEditor);
        channelTags.putIfAbsent("webMaster", Channel::setWebMaster);
        channelTags.putIfAbsent("docs", Channel::setDocs);
        channelTags.putIfAbsent("rating", Channel::setRating);
        channelTags.putIfAbsent("/rss/channel/image/link", (C c, String v) -> createIfNull(c::getImage, c::setImage, Image::new).setLink(v));
        channelTags.putIfAbsent("/rss/channel/image/title", (C c, String v) -> createIfNull(c::getImage, c::setImage, Image::new).setTitle(v));
        channelTags.putIfAbsent("/rss/channel/image/url", (C c, String v) -> createIfNull(c::getImage, c::setImage, Image::new).setUrl(v));
        channelTags.putIfAbsent("/rss/channel/image/description", (C c, String v) -> createIfNullOptional(c::getImage, c::setImage, Image::new).ifPresent(i -> i.setDescription(v)));
        channelTags.putIfAbsent("/rss/channel/image/height", (C c, String v) -> createIfNullOptional(c::getImage, c::setImage, Image::new).ifPresent(i -> mapInteger(v, i::setHeight)));
        channelTags.putIfAbsent("/rss/channel/image/width", (C c, String v) -> createIfNullOptional(c::getImage, c::setImage, Image::new).ifPresent(i -> mapInteger(v, i::setWidth)));
    }

    /**
     * Register channel attributes for mapping to channel object fields
     */
    protected void registerChannelAttributes() {
        channelAttributes.computeIfAbsent("link", k -> new HashMap<>()).put("href", Channel::setLink);
    }

    /**
     * Register item tags for mapping to item object fields
     */
    @SuppressWarnings("java:S1192")
    protected void registerItemTags() {
        itemTags.putIfAbsent("guid", Item::setGuid);
        itemTags.putIfAbsent("id", Item::setGuid);
        itemTags.putIfAbsent("title", Item::setTitle);
        itemTags.putIfAbsent("description", Item::setDescription);
        itemTags.putIfAbsent("summary", Item::setDescription);
        itemTags.putIfAbsent("content", Item::setDescription);
        itemTags.putIfAbsent("link", Item::setLink);
        itemTags.putIfAbsent("author", Item::setAuthor);
        itemTags.putIfAbsent("/feed/entry/author/name", Item::setAuthor);
        itemTags.putIfAbsent("category", Item::addCategory);
        itemTags.putIfAbsent("pubDate", Item::setPubDate);
        itemTags.putIfAbsent("published", Item::setPubDate);
        itemTags.putIfAbsent("updated", (i, v) -> {
            if (i.getPubDate().isPresent() &&
                    i.getPubDate().get().isEmpty()) {
                i.setPubDate(v);
            }
        });
        itemTags.putIfAbsent("comments", Item::setComments);
        itemTags.putIfAbsent("dc:creator", (i, v) -> Mapper.mapIfEmpty(v, i::getAuthor, i::setAuthor));
        itemTags.putIfAbsent("dc:date", (i, v) -> Mapper.mapIfEmpty(v, i::getPubDate, i::setPubDate));

        onItemTags.put("enclosure", i -> i.addEnclosure(new Enclosure()));
    }

    /**
     * Register itam attributes for mapping to item object fields
     */
    protected void registerItemAttributes() {
        itemAttributes.computeIfAbsent("link", k -> new HashMap<>()).putIfAbsent("href", Item::setLink);
        itemAttributes.computeIfAbsent("guid", k -> new HashMap<>()).putIfAbsent("isPermaLink", (i, v) -> i.setIsPermaLink(Boolean.parseBoolean(v)));

        var enclosureAttributes = itemAttributes.computeIfAbsent("enclosure", k -> new HashMap<>());
        enclosureAttributes.putIfAbsent("url", (i, v) -> i.getEnclosure().ifPresent(a -> a.setUrl(v)));
        enclosureAttributes.putIfAbsent("type", (i, v) -> i.getEnclosure().ifPresent(a -> a.setType(v)));
        enclosureAttributes.putIfAbsent("length", (i, v) -> i.getEnclosure().ifPresent(e -> mapLong(v, e::setLength)));
    }

    /**
     * Date and Time parser for parsing timestamps.
     *
     * @param dateTimeParser the date time parser to use.
     * @return updated RSSReader.
     */
    public WoodstoxAbstractRssReader<C, I> setDateTimeParser(DateTimeParser dateTimeParser) {
        Objects.requireNonNull(dateTimeParser, "Date time parser must not be null");

        this.dateTimeParser = dateTimeParser;
        return this;
    }

    /**
     * Adds a http header to the HttpClient.
     * This is completely optional and if no headers are set then it will not add anything.
     *
     * @param key   the key name of the header.
     * @param value the value of the header.
     * @return updated RSSReader.
     */
    public WoodstoxAbstractRssReader<C, I> addHeader(String key, String value) {
        Objects.requireNonNull(key, "Key must not be null");
        Objects.requireNonNull(value, "Value must not be null");

        this.headers.put(key, value);
        return this;
    }

    /**
     * Add item extension for tags
     *
     * @param tag      - tag name
     * @param consumer - setter method in Item class to use for mapping
     * @return this instance
     */
    public WoodstoxAbstractRssReader<C, I> addItemExtension(String tag, BiConsumer<I, String> consumer) {
        Objects.requireNonNull(tag, "Item tag must not be null");
        Objects.requireNonNull(consumer, "Item consumer must not be null");

        itemTags.put(tag, consumer);
        return this;
    }

    /**
     * Add item extension for attributes
     *
     * @param tag       - tag name
     * @param attribute - attribute name
     * @param consumer  - setter method in Item class to use for mapping
     * @return this instance
     */
    public WoodstoxAbstractRssReader<C, I> addItemExtension(String tag, String attribute, BiConsumer<I, String> consumer) {
        Objects.requireNonNull(tag, "Item tag must not be null");
        Objects.requireNonNull(attribute, "Item attribute must not be null");
        Objects.requireNonNull(consumer, "Item consumer must not be null");

        itemAttributes.computeIfAbsent(tag, k -> new HashMap<>())
                .put(attribute, consumer);
        return this;
    }

    /**
     * Add channel extension for tags
     *
     * @param tag      - tag name
     * @param consumer - setter method in Channel class to use for mapping
     * @return this instance
     */
    public WoodstoxAbstractRssReader<C, I> addChannelExtension(String tag, BiConsumer<C, String> consumer) {
        Objects.requireNonNull(tag, "Channel tag must not be null");
        Objects.requireNonNull(consumer, "Channel consumer must not be null");

        channelTags.put(tag, consumer);
        return this;
    }

    /**
     * Add channel extension for attributes
     *
     * @param tag       - tag name
     * @param attribute - attribute name
     * @param consumer  - setter method in Channel class to use for mapping
     * @return this instance
     */
    public WoodstoxAbstractRssReader<C, I> addChannelExtension(String tag, String attribute, BiConsumer<C, String> consumer) {
        Objects.requireNonNull(tag, "Channel tag must not be null");
        Objects.requireNonNull(attribute, "Channel attribute must not be null");
        Objects.requireNonNull(consumer, "Channel consumer must not be null");

        channelAttributes.computeIfAbsent(tag, k -> new HashMap<>())
                .put(attribute, consumer);
        return this;
    }

    /**
     * Read RSS feed with the given URL.
     *
     * @param url URL to RSS feed.
     * @return Stream of items
     */
    @SuppressWarnings("squid:S1181")
    public Stream<I> read(String url) {
        Objects.requireNonNull(url, "URL must not be null");

        try {
            return readAsync(url).get(15, TimeUnit.SECONDS);
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Read RSS feed asynchronous with the given URL.
     *
     * @param url URL to RSS feed.
     * @return Stream of items
     */
    public CancellableStreamFuture<I> readAsync(String url) {
        Objects.requireNonNull(url, "URL must not be null");

        if (!isInitialized) {
            initialize();
            isInitialized = true;
        }

        return sendAsyncRequest(url);
    }

    /**
     * Sends request
     *
     * @param url url
     * @return response
     */
    protected CancellableStreamFuture<I> sendAsyncRequest(String url) {
        var builder = new Request.Builder()
                .url(url)
                .header("Accept-Encoding", "gzip");

        headers.forEach(builder::header);

        Call call = httpClient.newCall(builder.build());
        CancellableStreamFuture<I> future = new CancellableStreamFuture<>(call);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                future.complete(null);
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                if (response.code() < 400 || response.code() >= 600) {
                    try {
                        var inputStream = response.body().byteStream();
                        if ("gzip".equals(response
                                .headers().get("Content-Encoding"))) {
                            inputStream = new GZIPInputStream(inputStream);
                        }

                        inputStream = new BufferedInputStream(inputStream);

                        removeBadData(inputStream);
                        var itemIterator = new RssItemIterator(inputStream);

                        future.complete(StreamSupport.stream(
                                Spliterators.spliteratorUnknownSize(itemIterator, Spliterator.ORDERED), false
                        ).onClose(itemIterator::close));
                    } catch (IOException exception) {
                        Log.e("AbstractRssReader", "onResponse: ", exception);

                        future.complete(null);
                    }
                }
            }
        });

        return future;
    }

    private void removeBadData(InputStream inputStream) throws IOException {
        inputStream.mark(2);
        var firstChar = inputStream.read();

        if (firstChar != 65279 && firstChar != 13 && firstChar != 10 && !Character.isWhitespace(firstChar)) {
            inputStream.reset();
        } else if (firstChar == 13 || Character.isWhitespace(firstChar)) {
            var secondChar = inputStream.read();

            if (secondChar != 10 && !Character.isWhitespace(secondChar)) {
                inputStream.reset();
                inputStream.read();
            }
        }
    }

    class RssItemIterator implements Iterator<I> {
        private final StringBuilder textBuilder;
        private final Map<String, StringBuilder> childNodeTextBuilder;
        private final InputStream is;
        private final Deque<String> elementStack;
        private XMLStreamReader reader;
        private C channel;
        private I item = null;
        private I nextItem;
        private boolean isChannelPart = false;
        private boolean isItemPart = false;

        public RssItemIterator(InputStream is) {
            this.is = is;
            nextItem = null;
            textBuilder = new StringBuilder();
            childNodeTextBuilder = new HashMap<>();
            elementStack = new ArrayDeque<>();

            try {
                var xmlInFact = XMLInputFactory.newInstance();

                // disable XML external entity (XXE) processing
                xmlInFact.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
                xmlInFact.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
                xmlInFact.setProperty(XMLInputFactory.IS_COALESCING, true);

                reader = xmlInFact.createXMLStreamReader(is);
            } catch (XMLStreamException e) {
                var logger = Logger.getLogger(LOG_GROUP);

                if (logger.isLoggable(Level.WARNING))
                    logger.log(Level.WARNING, "Failed to process XML. ", e);
            }
        }

        public void close() {
            try {
                reader.close();
                is.close();
            } catch (XMLStreamException | IOException e) {
                var logger = Logger.getLogger(LOG_GROUP);

                if (logger.isLoggable(Level.WARNING))
                    logger.log(Level.WARNING, "Failed to close XML stream. ", e);
            }
        }

        void peekNext() {
            if (nextItem == null) {
                try {
                    nextItem = next();
                } catch (NoSuchElementException e) {
                    nextItem = null;
                }
            }
        }

        @Override
        public boolean hasNext() {
            peekNext();
            return nextItem != null;
        }

        @Override
        @SuppressWarnings("squid:S3776")
        public I next() {
            if (nextItem != null) {
                var next = nextItem;
                nextItem = null;

                return next;
            }

            try {
                while (reader.hasNext()) {
                    var type = reader.next();

                    collectChildNodes(type);

                    if (type == CHARACTERS || type == CDATA) {
                        parseCharacters();
                    } else if (type == START_ELEMENT) {
                        parseStartElement();
                        parseAttributes();
                    } else if (type == END_ELEMENT) {
                        var itemParsed = parseEndElement();

                        if (itemParsed) {
                            return item;
                        }
                    }
                }
            } catch (XMLStreamException e) {
                var logger = Logger.getLogger(LOG_GROUP);

                if (logger.isLoggable(Level.WARNING))
                    logger.log(Level.WARNING, "Failed to parse XML. ", e);
            }

            close();
            throw new NoSuchElementException();
        }

        void collectChildNodes(int type) {
            if (type == START_ELEMENT) {
                var nsTagName = toNsName(reader.getPrefix(), reader.getLocalName());

                if (!childNodeTextBuilder.isEmpty()) {
                    StringBuilder startTagBuilder = new StringBuilder("<").append(nsTagName);
                    // Add namespaces to start tag
                    for (int i = 0; i < reader.getNamespaceCount(); ++i) {
                        startTagBuilder.append(" ")
                                .append(toNamespacePrefix(reader.getNamespacePrefix(i)))
                                .append("=")
                                .append(reader.getNamespaceURI(i));
                    }
                    // Add attributes to start tag
                    for (int i = 0; i < reader.getAttributeCount(); ++i) {
                        startTagBuilder.append(" ")
                                .append(toNsName(reader.getAttributePrefix(i), reader.getAttributeLocalName(i)))
                                .append("=")
                                .append(reader.getAttributeValue(i));
                    }
                    startTagBuilder.append(">");
                    var startTag = startTagBuilder.toString();

                    childNodeTextBuilder.entrySet()
                            .stream()
                            .filter(e -> !e.getKey().equals(nsTagName))
                            .forEach(e -> e.getValue().append(startTag));
                }

                // Collect child notes for tag names in this set
                if (collectChildNodesForTag.contains(nsTagName)) {
                    childNodeTextBuilder.put(nsTagName, new StringBuilder());
                }
            } else if (type == CHARACTERS || type == CDATA) {
                childNodeTextBuilder.forEach((k, builder) -> builder.append(reader.getText()));
            } else if (type == END_ELEMENT) {
                var nsTagName = toNsName(reader.getPrefix(), reader.getLocalName());
                var endTag = "</" + nsTagName + ">";
                childNodeTextBuilder.entrySet()
                        .stream()
                        .filter(e -> !e.getKey().equals(nsTagName))
                        .forEach(e -> e.getValue().append(endTag));
            }
        }

        @SuppressWarnings("java:S5738")
        void parseStartElement() {
            textBuilder.setLength(0);
            var nsTagName = toNsName(reader.getPrefix(), reader.getLocalName());
            elementStack.addLast(nsTagName);

            if (isChannel(nsTagName)) {
                channel = Objects.requireNonNullElse(createChannel(dateTimeParser), createChannel());
                channel.setTitle("");
                channel.setDescription("");
                channel.setLink("");
                isChannelPart = true;
            } else if (isItem(nsTagName)) {
                item = Objects.requireNonNullElse(createItem(dateTimeParser), createItem());
                item.setChannel(channel);
                isChannelPart = false;
                isItemPart = true;
            }
        }

        protected boolean isChannel(String tagName) {
            return "channel".equals(tagName) || "feed".equals(tagName);
        }

        protected boolean isItem(String tagName) {
            return "item".equals(tagName) || "entry".equals(tagName);
        }

        void parseAttributes() {
            var nsTagName = toNsName(reader.getPrefix(), reader.getLocalName());
            var elementFullPath = getElementFullPath();

            if (isChannelPart) {
                // Map channel attributes
                mapChannelAttributes(nsTagName);
                mapChannelAttributes(elementFullPath);
            } else if (isItemPart) {
                onItemTags.computeIfPresent(nsTagName, (k, f) -> {
                    f.accept(item);
                    return f;
                });
                onItemTags.computeIfPresent(getElementFullPath(), (k, f) -> {
                    f.accept(item);
                    return f;
                });
                // Map item attributes
                mapItemAttributes(nsTagName);
                mapItemAttributes(elementFullPath);
            }
        }

        void mapChannelAttributes(String key) {
            var consumers = channelAttributes.get(key);
            if (consumers != null && channel != null) {
                consumers.forEach((attributeName, consumer) -> {
                    var attributeValue = Optional.ofNullable(reader.getAttributeValue(null, attributeName));
                    attributeValue.ifPresent(v -> consumer.accept(channel, v));
                });
            }
        }

        void mapItemAttributes(String key) {
            var consumers = itemAttributes.get(key);
            if (consumers != null && item != null) {
                consumers.forEach((attributeName, consumer) -> {
                    var attributeValue = Optional.ofNullable(reader.getAttributeValue(null, attributeName));
                    attributeValue.ifPresent(v -> consumer.accept(item, v));
                });
            }
        }

        boolean parseEndElement() {
            var nsTagName = toNsName(reader.getPrefix(), reader.getLocalName());
            var text = textBuilder.toString().trim();
            var elementFullPath = getElementFullPath();
            elementStack.removeLast();

            if (isChannelPart)
                parseChannelCharacters(channel, nsTagName, elementFullPath, text);
            else
                parseItemCharacters(item, nsTagName, elementFullPath, text);

            textBuilder.setLength(0);

            return isItem(nsTagName);
        }

        void parseCharacters() {
            var text = reader.getText();

            if (text.isBlank())
                return;

            textBuilder.append(text);
        }

        void parseChannelCharacters(C channel, String nsTagName, String elementFullPath, String text) {
            if (channel == null || text.isEmpty())
                return;

            channelTags.computeIfPresent(nsTagName, (k, f) -> {
                f.accept(channel, text);
                return f;
            });
            channelTags.computeIfPresent(elementFullPath, (k, f) -> {
                f.accept(channel, text);
                return f;
            });
        }

        void parseItemCharacters(final I item, String nsTagName, String elementFullPath, final String text) {
            var builder = childNodeTextBuilder.remove(nsTagName);
            if (item == null || (text.isEmpty() && builder == null))
                return;

            var textValue = (builder != null) ? builder.toString().trim() : text;
            itemTags.computeIfPresent(nsTagName, (k, f) -> {
                f.accept(item, textValue);
                return f;
            });
            itemTags.computeIfPresent(elementFullPath, (k, f) -> {
                f.accept(item, text);
                return f;
            });
        }

        String toNsName(String prefix, String name) {
            return prefix.isEmpty() ? name : prefix + ":" + name;
        }

        String toNamespacePrefix(String prefix) {
            return prefix == null || prefix.isEmpty() ? "xmlns" : "xmlns" + ":" + prefix;
        }

        String getElementFullPath() {
            return "/" + String.join("/", elementStack);
        }
    }

    private OkHttpClient createHttpClient() {
        OkHttpClient client;

        try {
            var context = SSLContext.getInstance("TLSv1.2");
            context.init(null, null, null);

            client = new OkHttpClient.Builder()
                    .sslSocketFactory(context.getSocketFactory(),
                            TrustManagerLoader.getDefaultX509TrustManager())
                    .followSslRedirects(true)
                    .followRedirects(true)
                    .connectTimeout(Duration.ofSeconds(25))
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException exception) {
            client = new OkHttpClient.Builder()
                    .followRedirects(true)
                    .connectTimeout(Duration.ofSeconds(25))
                    .build();
        }

        return client;
    }

    public static class CancellableStreamFuture<I extends Item> extends CompletableFuture<Stream<I>> {
        private final Call call;

        public CancellableStreamFuture(Call call) {
            this.call = call;
        }

        public void cancelCall() {
            if (call != null && !call.isCanceled()) {
                call.cancel();
            }
        }
    }
}