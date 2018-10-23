/*
 * Copyright © 2018 Tinkoff Bank
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.tinkoff;

/*
 * @author Snezhana Krass
 */

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.conversions.Bson;
import ru.tinkoff.objects.Snapshot;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Accumulators.push;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;
import static java.util.Arrays.asList;

public class SnapshotStorage {

    private MongoDatabase db;
    private MongoCollection<Document> collection;
    private static final String DB_NAME = "visualapp";
    private static final String DB_HOST = System.getProperty("mongodb.host");
    private static final int DB_PORT = Integer.parseInt(System.getProperty("mongodb.port"));

    private static SnapshotStorage instance = null;

    public static SnapshotStorage getInstance() {
        if (instance == null) {
            instance = new SnapshotStorage();
        }
        return instance;
    }

    private SnapshotStorage() {
        this.db = mongo();
        this.collection = db.getCollection("snapshots");
    }

    public List<Document> aggregate(String actualDate, String expectedDate) {
        DBObject pushFields = new BasicDBObject();
        pushFields.put("server", "$server");
        pushFields.put("branch", "$branch");
        pushFields.put("commit", "$commit");
        pushFields.put("testcaseId", "$testcaseId");
        pushFields.put("story", "$story");
        pushFields.put("state", "$state");
        pushFields.put("datetime", "$datetime");
        pushFields.put("elements", "$elements");
        pushFields.put("url", "$url");
        pushFields.put("device", "$device");
        pushFields.put("osName", "$osName");
        pushFields.put("osVersion", "$osVersion");
        pushFields.put("browserName", "$browserName");
        pushFields.put("browserVersion", "$browserVersion");
        pushFields.put("resolution", "$resolution");
        pushFields.put("retina", "$retina");

        List<Document> documents = collection.aggregate(asList(
                match(regex("_id", "^(" + actualDate + "|" + expectedDate + ")")),
                group("$hash", push("snapshot", pushFields))))
                .batchSize(500)
                .allowDiskUse(true)
                .into(new ArrayList<>());

        return documents;
    }

    public List<Document> find() {
        List<Bson> matchFilters = new ArrayList<>();

        DBObject groupFields = new BasicDBObject();
        groupFields.put("datetime", "$datetime");

        DBObject pushFields = new BasicDBObject();
        pushFields.put("server", "$server");
        pushFields.put("branch", "$branch");
        pushFields.put("commit", "$commit");
        pushFields.put("testcaseId", "$testcaseId");
        pushFields.put("story", "$story");
        pushFields.put("state", "$state");
        pushFields.put("url", "$url");

        List<Bson> pipeline = new ArrayList<>();
        if (matchFilters.size() > 0) {
            pipeline.add(match(and(matchFilters)));
        }
        pipeline.add(group(new BasicDBObject("_id", groupFields), push("snapshot", pushFields)));

        List<Document> documents = collection.aggregate(pipeline)
                .allowDiskUse(true)
                .into(new ArrayList<>());

        return documents;
    }

    public List<Snapshot> find(String datetime) {
        List<Snapshot> snapshots = new ArrayList<>();
        collection.find(eq("datetime", Date.from(Instant.parse(datetime))))
                .forEach((Block<? super Document>) d -> snapshots.add(new Snapshot(d)));
        return snapshots;
    }

    public void create(Snapshot snapshot) {
        Document updateFields = new Document()
                .append("elements", snapshot.getElements())
                .append("url", snapshot.getUrl());

        String hash = snapshot.getHash();
        String id = snapshot.getDatetime().toInstant() + "|&datetime&|" + hash;

        Document insertFields = new Document()
                .append("_id", id)
                .append("hash", hash)
                .append("branch", snapshot.getBranch())
                .append("server", snapshot.getServer())
                .append("commit", snapshot.getCommit())
                .append("datetime", snapshot.getDatetime())
                .append("testcaseId", snapshot.getTestcaseId())
                .append("story", snapshot.getStory())
                .append("state", snapshot.getState())
                .append("elements", snapshot.getElements())
                .append("url", snapshot.getUrl())
                .append("device", snapshot.getDevice())
                .append("osName", snapshot.getOsName())
                .append("osVersion", snapshot.getOsVersion())
                .append("browserName", snapshot.getBrowserName())
                .append("browserVersion", snapshot.getBrowserVersion())
                .append("resolution", snapshot.getResolution())
                .append("retina", snapshot.isRetina());

        Document founded = collection.find(eq("_id", id)).first();
        if (founded != null) {
            collection.updateOne(new BasicDBObject("_id", id), new Document("$set", updateFields));
        } else {
            collection.insertOne(insertFields);
        }
    }

    private MongoDatabase mongo() {
        MongoClient mongoClient = new MongoClient(DB_HOST, DB_PORT);
        return mongoClient.getDatabase(DB_NAME);
    }
}