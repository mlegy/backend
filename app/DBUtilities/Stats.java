package DBUtilities;

import com.couchbase.client.core.BackpressureException;
import com.couchbase.client.core.BucketClosedException;
import com.couchbase.client.core.CouchbaseException;
import com.couchbase.client.core.time.Delay;
import com.couchbase.client.deps.io.netty.handler.timeout.TimeoutException;
import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import com.couchbase.client.java.error.CASMismatchException;
import com.couchbase.client.java.error.DocumentAlreadyExistsException;
import com.couchbase.client.java.error.DocumentDoesNotExistException;
import com.couchbase.client.java.error.TemporaryFailureException;
import com.couchbase.client.java.query.AsyncN1qlQueryResult;
import com.couchbase.client.java.query.N1qlQuery;
import com.couchbase.client.java.query.dsl.Expression;
import com.couchbase.client.java.util.retry.RetryBuilder;
import play.Logger;
import rx.Observable;

import java.util.concurrent.TimeUnit;

import static com.couchbase.client.java.query.Update.update;

/**
 * Created by rashwan on 3/29/16.
 */
public class Stats {
    private static AsyncBucket mBucket;
    private static final Logger.ALogger logger = Logger.of (Stats.class.getSimpleName ());

    /**
     * Create and save a project's stats. can error with {@link CouchbaseException},{@link DocumentAlreadyExistsException} and {@link BucketClosedException}.
     * @param statsId the id of the stats document to create.
     * @return an observable of the created Json document.
     */
    public static Observable<JsonObject> createStats(String statsId,JsonObject statsObject){
        try {
            checkDBStatus();
        } catch (BucketClosedException e) {
            return Observable.error(e);
        }

        JsonDocument statsDocument = JsonDocument.create (statsId,statsObject);

        return mBucket.insert (statsDocument).single ().timeout (500, TimeUnit.MILLISECONDS)
            .retryWhen (RetryBuilder.anyOf (TemporaryFailureException.class, BackpressureException.class)
                    .delay (Delay.fixed (200, TimeUnit.MILLISECONDS)).max (3).build ())
            .retryWhen (RetryBuilder.anyOf (TimeoutException.class)
                    .delay (Delay.fixed (500,TimeUnit.MILLISECONDS)).once ().build ())
            .onErrorResumeNext (throwable -> {
                if (throwable instanceof DocumentAlreadyExistsException) {
                    return Observable.error (new DocumentAlreadyExistsException ("Failed to create stats, ID already exists"));
                } else {
                    return Observable.error (new CouchbaseException ("Failed to create stats, General DB exception "));
                }
            }).flatMap (jsonDocument -> Observable.just (jsonDocument.content ().put ("id",jsonDocument.id ())));

    }

    /**
     * Get stats for a project using its id. can error with {@link CouchbaseException} and {@link BucketClosedException}.
     * @param statsId the id of the stats document to get.
     * @return an observable of the json document if it was found , if it wasn't found it returns an empty json document with id DBConfig.EMPTY_JSON_DOC .
     */
    public static Observable<JsonObject> getStatsWithId(String statsId){
        try {
            checkDBStatus();
        } catch (BucketClosedException e) {
            return Observable.error(e);
        }

        return mBucket.get (statsId).timeout (500,TimeUnit.MILLISECONDS)
            .retryWhen (RetryBuilder.anyOf (TemporaryFailureException.class, BackpressureException.class)
                .delay (Delay.fixed (200, TimeUnit.MILLISECONDS)).max (3).build ())
            .retryWhen (RetryBuilder.anyOf (TimeoutException.class)
                .delay (Delay.fixed (500,TimeUnit.MILLISECONDS)).once ().build ())
            .onErrorResumeNext (throwable -> {
                return Observable.error (new CouchbaseException ("Failed to get stats, General DB exception"));
            })
            .defaultIfEmpty (JsonDocument.create (DBConfig.EMPTY_JSON_DOC,JsonObject.create ()))
            .flatMap (jsonDocument -> Observable.just (jsonDocument.content ().put ("id",jsonDocument.id ())));
    }
    /**
     * Adds 1 to the enrollments count of the stats with the provided ID.
     * @param statsId The ID of the stats to update.
     * @return An observable of Json object containing the stats id and the new enrollments count.
     */
    public static Observable<JsonObject> add1ToStatsEnrollmentsCount(String statsId){
        try {
            checkDBStatus();
        } catch (BucketClosedException e) {
            return Observable.error(e);
        }

        Logger.info ("DB: Adding 1 to contributions count of stats with id: {}",statsId);

        return mBucket.query (N1qlQuery.simple (update (Expression.x (DBConfig.BUCKET_NAME + " stats")).useKeys (Expression.s (statsId))
        .set ("enrollments_count",Expression.x ("enrollments_count + " + 1 ))
        .returning (Expression.x ("enrollments_count, meta(stats).id"))))
        .flatMap (AsyncN1qlQueryResult::rows).flatMap (row -> Observable.just (row.value ()))
        .retryWhen (RetryBuilder.anyOf (TemporaryFailureException.class, BackpressureException.class)
                .delay (Delay.fixed (200, TimeUnit.MILLISECONDS)).max (3).build ())
        .retryWhen (RetryBuilder.anyOf (TimeoutException.class)
                .delay (Delay.fixed (500,TimeUnit.MILLISECONDS)).once ().build ())
        .onErrorResumeNext (throwable -> {
            if (throwable instanceof CASMismatchException){
                //// TODO: 4/1/16 needs more accurate handling in the future.
                logger.info ("DB: Failed to add 1 to enrollments count of stats with id: {}",statsId);

                return Observable.error (new CASMismatchException (String.format ("DB: Failed to add 1 to enrollments count of stats with id: $1, General DB exception.",statsId)));
            } else {
                logger.info ("DB: Failed to add 1 to enrollments count of stats with id: {}",statsId);

                return Observable.error (new CouchbaseException (String.format ("DB: Failed to add 1 to enrollments count of stats with id: $1, General DB exception.",statsId)));
            }
        }).defaultIfEmpty (JsonObject.create ().put ("id",DBConfig.EMPTY_JSON_DOC));
    }

    /**
     * Remove 1 from the enrollments count of the stats with the provided ID.
     * @param statsId The ID of the stats to update.
     * @return An observable of Json object containing the stats id and the new enrollments count.
     */
    public static Observable<JsonObject> remove1FromStatsEnrollmentsCount(String statsId){
        try {
            checkDBStatus();
        } catch (BucketClosedException e) {
            return Observable.error(e);
        }

        Logger.info ("DB: Removing 1 from contributions count of stats with id: {}",statsId);

        return mBucket.query (N1qlQuery.simple (update (Expression.x (DBConfig.BUCKET_NAME + " stats")).useKeys (Expression.s (statsId))
        .set ("enrollments_count",Expression.x ("enrollments_count - " + 1 ))
        .returning (Expression.x ("enrollments_count, meta(stats).id"))))
        .flatMap (AsyncN1qlQueryResult::rows).flatMap (row -> Observable.just (row.value ()))
        .retryWhen (RetryBuilder.anyOf (TemporaryFailureException.class, BackpressureException.class)
                .delay (Delay.fixed (200, TimeUnit.MILLISECONDS)).max (3).build ())
        .retryWhen (RetryBuilder.anyOf (TimeoutException.class)
                .delay (Delay.fixed (500,TimeUnit.MILLISECONDS)).once ().build ())
        .onErrorResumeNext (throwable -> {
            if (throwable instanceof CASMismatchException){
                //// TODO: 4/1/16 needs more accurate handling in the future.
                logger.info ("DB: Failed to remove 1 from enrollments count of stats with id: {}",statsId);

                return Observable.error (new CASMismatchException (String.format ("DB: Failed to remove 1 from enrollments count of stats with id: $1, General DB exception.",statsId)));
            } else {
                logger.info ("DB: Failed to remove 1 from enrollments count of stats with id: {}",statsId);

                return Observable.error (new CouchbaseException (String.format ("DB: Failed to remove 1 from enrollments count of stats with id: $1, General DB exception.",statsId)));
            }
        }).defaultIfEmpty (JsonObject.create ().put ("id",DBConfig.EMPTY_JSON_DOC));
    }

    /**
     * Update stats of a project. can error with {@link CouchbaseException},{@link DocumentDoesNotExistException},{@link CASMismatchException} and {@link BucketClosedException} .
     * @param statsId The id of the stats document to be updated .
     * @param statsJsonObject The updated Json object to be used as the value of updated document.
     * @return an observable of the updated Json document .
     */
    public static Observable<JsonDocument> updateStats(String statsId,JsonObject statsJsonObject){
        try {
            checkDBStatus();
        } catch (BucketClosedException e) {
            return Observable.error(e);
        }

        JsonDocument statsDocument = JsonDocument.create (statsId,DBConfig.removeIdFromJson (statsJsonObject));

        return mBucket.replace (statsDocument).timeout (500,TimeUnit.MILLISECONDS)
            .retryWhen (RetryBuilder.anyOf (TemporaryFailureException.class, BackpressureException.class)
                    .delay (Delay.fixed (200, TimeUnit.MILLISECONDS)).max (3).build ())
            .retryWhen (RetryBuilder.anyOf (TimeoutException.class)
                    .delay (Delay.fixed (500,TimeUnit.MILLISECONDS)).once ().build ())
            .onErrorResumeNext (throwable -> {
                if (throwable instanceof DocumentDoesNotExistException){
                    return Observable.error (new DocumentDoesNotExistException ("Failed to update stats, ID dosen't exist in DB"));

                }else if (throwable instanceof CASMismatchException){
                    //// TODO: 3/28/16 needs more accurate handling in the future.
                    return Observable.error (new CASMismatchException ("Failed to update stats, CAS value is changed"));
                }
                else {
                    return Observable.error (new CouchbaseException ("Failed to update stats, General DB exception "));
                }
            });
    }

    /**
     * Delete stats of a project using its id. can error with {@link CouchbaseException}, {@link DocumentDoesNotExistException} and {@link BucketClosedException} .
     * @param statsId The id of the stats document to be deleted.
     * @return An observable with Json document containing only the id .
     */

    public static Observable<JsonDocument> deleteStats(String statsId){
        try {
            checkDBStatus();
        } catch (BucketClosedException e) {
            return Observable.error(e);
        }

        return mBucket.remove (statsId).timeout (500, TimeUnit.MILLISECONDS)
            .retryWhen (RetryBuilder.anyOf (TemporaryFailureException.class, BackpressureException.class)
                    .delay (Delay.fixed (200, TimeUnit.MILLISECONDS)).max (3).build ())
            .retryWhen (RetryBuilder.anyOf (TimeoutException.class)
                    .delay (Delay.fixed (500, TimeUnit.MILLISECONDS)).once ().build ())
            .onErrorResumeNext (throwable -> {
                if (throwable instanceof DocumentDoesNotExistException) {
                    return Observable.error (new DocumentDoesNotExistException ("Failed to delete stats, ID dosen't exist in DB"));
                } else {
                    return Observable.error (new CouchbaseException ("Failed to delete stats, General DB exception "));
                }
            });
    }

    private static void checkDBStatus () {
        if (DBConfig.bucket.isClosed ()){
            if (DBConfig.initDB() == DBConfig.OPEN_BUCKET_OK) {
                mBucket = DBConfig.bucket;
            }else{
                throw new BucketClosedException ("Failed to open bucket due to timeout or backpressure");
            }
        }else {
            mBucket = DBConfig.bucket;
        }
    }
}
