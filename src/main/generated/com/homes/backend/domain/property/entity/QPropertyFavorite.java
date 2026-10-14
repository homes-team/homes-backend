package com.homes.backend.domain.property.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPropertyFavorite is a Querydsl query type for PropertyFavorite
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPropertyFavorite extends EntityPathBase<PropertyFavorite> {

    private static final long serialVersionUID = -1141249108L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPropertyFavorite propertyFavorite = new QPropertyFavorite("propertyFavorite");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QProperty property;

    public final com.homes.backend.domain.user.entity.QUser user;

    public QPropertyFavorite(String variable) {
        this(PropertyFavorite.class, forVariable(variable), INITS);
    }

    public QPropertyFavorite(Path<? extends PropertyFavorite> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPropertyFavorite(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPropertyFavorite(PathMetadata metadata, PathInits inits) {
        this(PropertyFavorite.class, metadata, inits);
    }

    public QPropertyFavorite(Class<? extends PropertyFavorite> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.property = inits.isInitialized("property") ? new QProperty(forProperty("property"), inits.get("property")) : null;
        this.user = inits.isInitialized("user") ? new com.homes.backend.domain.user.entity.QUser(forProperty("user")) : null;
    }

}

