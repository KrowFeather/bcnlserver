package zxylearn.bcnlserver.ES;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.stereotype.Service;

import org.springframework.data.elasticsearch.core.query.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import zxylearn.bcnlserver.pojo.document.TweetDoc;

@Service
public class TweetDocService {

    @Autowired
    private ElasticsearchOperations esOps;

    // 创建推文
    public boolean createTweet(TweetDoc tweetDoc) {
        return esOps.save(tweetDoc) != null;
    }

    public TweetDoc getTweetDoc(Long TweetId) {
        return esOps.get(TweetId.toString(), TweetDoc.class);
    }

    // 删除推文
    public boolean removeTweet(Long tweetId) {
        return esOps.delete(tweetId.toString(), TweetDoc.class) != null;
    }

    // 获取所有推文
    public List<TweetDoc> getTweetList() {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.matchAll(m -> m))
                .withSort(s -> s.field(f -> f
                        .field("createTime")
                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                ))
                .build();

        return esOps.search(query, TweetDoc.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }

    // 获取团队所有推文
    public List<TweetDoc> getTweetListByTeamId(Long teamId) {
        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q
                        .term(t -> t
                                .field("teamId")
                                .value(teamId)))
                .withSort(s -> s.field(f -> f
                        .field("createTime")
                        .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                ))
                .build();

        return esOps.search(query, TweetDoc.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }

    // 查询推文
    public List<TweetDoc> searchTweet(String keyword, Long teamId) {
        if(keyword == null && teamId == null) {
           return getTweetList();
        }


        if (keyword == null || keyword.isEmpty()) {
            return getTweetListByTeamId(teamId);
        }

        NativeQuery query;
        if (teamId == null) {
            query = NativeQuery.builder()
                    .withQuery(q -> q
                            .multiMatch(mm -> mm
                                    .query(keyword)
                                    .fields("title^2", "content^1")
                                    .type(TextQueryType.BestFields)))
                    .withSort(s -> s.field(f -> f
                            .field("createTime")
                            .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                    ))
                    .build();
        } else {
            query = NativeQuery.builder()
                    .withQuery(q -> q
                            .bool(b -> {
                                b.must(m -> m
                                        .multiMatch(mm -> mm
                                                .query(keyword)
                                                .fields("title^2", "content^1")
                                                .type(TextQueryType.BestFields)));
                                b.must(m -> m
                                        .term(t -> t
                                                .field("teamId")
                                                .value(teamId)));
                                return b;
                            }))
                    .withSort(s -> s.field(f -> f
                            .field("createTime")
                            .order(co.elastic.clients.elasticsearch._types.SortOrder.Desc)
                    ))
                    .build();
        }

        return esOps.search(query, TweetDoc.class)
                .stream()
                .map(SearchHit::getContent)
                .toList();
    }
}