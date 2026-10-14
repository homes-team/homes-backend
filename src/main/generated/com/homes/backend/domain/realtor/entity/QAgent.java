package com.homes.backend.domain.realtor.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAgent is a Querydsl query type for Agent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAgent extends EntityPathBase<Agent> {

    private static final long serialVersionUID = -1243794728L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAgent agent = new QAgent("agent");

    public final com.homes.backend.global.common.QBaseEntity _super = new com.homes.backend.global.common.QBaseEntity(this);

    public final StringPath agentCertUrl = createString("agentCertUrl");

    public final StringPath businessCertUrl = createString("businessCertUrl");

    public final StringPath businessNum = createString("businessNum");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isVerified = createBoolean("isVerified");

    public final StringPath officeAddress = createString("officeAddress");

    public final NumberPath<Double> officeLatitude = createNumber("officeLatitude", Double.class);

    public final NumberPath<Double> officeLongitude = createNumber("officeLongitude", Double.class);

    public final StringPath officeName = createString("officeName");

    public final StringPath profileImageUrl = createString("profileImageUrl");

    public final NumberPath<Float> reputationScore = createNumber("reputationScore", Float.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.homes.backend.domain.user.entity.QUser user;

    public QAgent(String variable) {
        this(Agent.class, forVariable(variable), INITS);
    }

    public QAgent(Path<? extends Agent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAgent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAgent(PathMetadata metadata, PathInits inits) {
        this(Agent.class, metadata, inits);
    }

    public QAgent(Class<? extends Agent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.homes.backend.domain.user.entity.QUser(forProperty("user")) : null;
    }

}

