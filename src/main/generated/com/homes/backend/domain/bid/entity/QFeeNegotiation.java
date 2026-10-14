package com.homes.backend.domain.bid.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFeeNegotiation is a Querydsl query type for FeeNegotiation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFeeNegotiation extends EntityPathBase<FeeNegotiation> {

    private static final long serialVersionUID = 83062924L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFeeNegotiation feeNegotiation = new QFeeNegotiation("feeNegotiation");

    public final com.homes.backend.global.common.QBaseEntity _super = new com.homes.backend.global.common.QBaseEntity(this);

    public final com.homes.backend.domain.realtor.entity.QAgent agent;

    public final QBid bid;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<BidStatus> status = createEnum("status", BidStatus.class);

    public final NumberPath<Double> suggestedFee = createNumber("suggestedFee", Double.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.homes.backend.domain.user.entity.QUser user;

    public QFeeNegotiation(String variable) {
        this(FeeNegotiation.class, forVariable(variable), INITS);
    }

    public QFeeNegotiation(Path<? extends FeeNegotiation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFeeNegotiation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFeeNegotiation(PathMetadata metadata, PathInits inits) {
        this(FeeNegotiation.class, metadata, inits);
    }

    public QFeeNegotiation(Class<? extends FeeNegotiation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.agent = inits.isInitialized("agent") ? new com.homes.backend.domain.realtor.entity.QAgent(forProperty("agent"), inits.get("agent")) : null;
        this.bid = inits.isInitialized("bid") ? new QBid(forProperty("bid"), inits.get("bid")) : null;
        this.user = inits.isInitialized("user") ? new com.homes.backend.domain.user.entity.QUser(forProperty("user")) : null;
    }

}

