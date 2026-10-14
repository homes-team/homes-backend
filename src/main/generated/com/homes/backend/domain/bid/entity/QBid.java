package com.homes.backend.domain.bid.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBid is a Querydsl query type for Bid
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBid extends EntityPathBase<Bid> {

    private static final long serialVersionUID = 664581516L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBid bid = new QBid("bid");

    public final com.homes.backend.global.common.QBaseEntity _super = new com.homes.backend.global.common.QBaseEntity(this);

    public final com.homes.backend.domain.realtor.entity.QAgent agent;

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Double> finalFee = createNumber("finalFee", Double.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final com.homes.backend.domain.property.entity.QProperty property;

    public final NumberPath<Double> proposedFee = createNumber("proposedFee", Double.class);

    public final EnumPath<BidStatus> status = createEnum("status", BidStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QBid(String variable) {
        this(Bid.class, forVariable(variable), INITS);
    }

    public QBid(Path<? extends Bid> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBid(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBid(PathMetadata metadata, PathInits inits) {
        this(Bid.class, metadata, inits);
    }

    public QBid(Class<? extends Bid> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.agent = inits.isInitialized("agent") ? new com.homes.backend.domain.realtor.entity.QAgent(forProperty("agent"), inits.get("agent")) : null;
        this.property = inits.isInitialized("property") ? new com.homes.backend.domain.property.entity.QProperty(forProperty("property"), inits.get("property")) : null;
    }

}

