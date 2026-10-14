package com.homes.backend.domain.property.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRecentView is a Querydsl query type for RecentView
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRecentView extends EntityPathBase<RecentView> {

    private static final long serialVersionUID = 779060475L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRecentView recentView = new QRecentView("recentView");

    public final com.homes.backend.global.common.QBaseEntity _super = new com.homes.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QProperty property;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.homes.backend.domain.user.entity.QUser user;

    public final DateTimePath<java.time.LocalDateTime> viewedAt = createDateTime("viewedAt", java.time.LocalDateTime.class);

    public QRecentView(String variable) {
        this(RecentView.class, forVariable(variable), INITS);
    }

    public QRecentView(Path<? extends RecentView> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRecentView(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRecentView(PathMetadata metadata, PathInits inits) {
        this(RecentView.class, metadata, inits);
    }

    public QRecentView(Class<? extends RecentView> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.property = inits.isInitialized("property") ? new QProperty(forProperty("property"), inits.get("property")) : null;
        this.user = inits.isInitialized("user") ? new com.homes.backend.domain.user.entity.QUser(forProperty("user")) : null;
    }

}

