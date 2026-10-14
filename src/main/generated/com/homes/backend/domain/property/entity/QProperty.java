package com.homes.backend.domain.property.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProperty is a Querydsl query type for Property
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProperty extends EntityPathBase<Property> {

    private static final long serialVersionUID = -1403867664L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProperty property = new QProperty("property");

    public final com.homes.backend.global.common.QBaseEntity _super = new com.homes.backend.global.common.QBaseEntity(this);

    public final StringPath address = createString("address");

    public final NumberPath<Integer> aiScore = createNumber("aiScore", Integer.class);

    public final NumberPath<Double> area = createNumber("area", Double.class);

    public final ComparablePath<org.locationtech.jts.geom.Point> coordinate = createComparable("coordinate", org.locationtech.jts.geom.Point.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Integer> currentFloor = createNumber("currentFloor", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> dealCompletedAt = createDateTime("dealCompletedAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> deposit = createNumber("deposit", Long.class);

    public final StringPath description = createString("description");

    public final NumberPath<Double> desiredBrokerageFee = createNumber("desiredBrokerageFee", Double.class);

    public final StringPath detailAddress = createString("detailAddress");

    public final NumberPath<Integer> favoriteCount = createNumber("favoriteCount", Integer.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final ListPath<PropertyImage, QPropertyImage> images = this.<PropertyImage, QPropertyImage>createList("images", PropertyImage.class, QPropertyImage.class, PathInits.DIRECT2);

    public final BooleanPath isSuspicious = createBoolean("isSuspicious");

    public final NumberPath<Long> maintenanceFee = createNumber("maintenanceFee", Long.class);

    public final NumberPath<Long> monthlyRent = createNumber("monthlyRent", Long.class);

    public final EnumPath<PropertyType> propertyType = createEnum("propertyType", PropertyType.class);

    public final NumberPath<Integer> reportCount = createNumber("reportCount", Integer.class);

    public final EnumPath<PropertyStatus> status = createEnum("status", PropertyStatus.class);

    public final ListPath<String, StringPath> tags = this.<String, StringPath>createList("tags", String.class, StringPath.class, PathInits.DIRECT2);

    public final StringPath title = createString("title");

    public final NumberPath<Integer> totalFloors = createNumber("totalFloors", Integer.class);

    public final EnumPath<TradeType> tradeType = createEnum("tradeType", TradeType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final com.homes.backend.domain.user.entity.QUser user;

    public QProperty(String variable) {
        this(Property.class, forVariable(variable), INITS);
    }

    public QProperty(Path<? extends Property> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProperty(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProperty(PathMetadata metadata, PathInits inits) {
        this(Property.class, metadata, inits);
    }

    public QProperty(Class<? extends Property> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.homes.backend.domain.user.entity.QUser(forProperty("user")) : null;
    }

}

