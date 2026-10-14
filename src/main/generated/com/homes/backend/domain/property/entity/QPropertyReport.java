package com.homes.backend.domain.property.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPropertyReport is a Querydsl query type for PropertyReport
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPropertyReport extends EntityPathBase<PropertyReport> {

    private static final long serialVersionUID = 1717943108L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPropertyReport propertyReport = new QPropertyReport("propertyReport");

    public final com.homes.backend.global.common.QBaseEntity _super = new com.homes.backend.global.common.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath customReason = createString("customReason");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final QProperty property;

    public final EnumPath<ReportReason> reason = createEnum("reason", ReportReason.class);

    public final com.homes.backend.domain.user.entity.QUser reporter;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QPropertyReport(String variable) {
        this(PropertyReport.class, forVariable(variable), INITS);
    }

    public QPropertyReport(Path<? extends PropertyReport> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPropertyReport(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPropertyReport(PathMetadata metadata, PathInits inits) {
        this(PropertyReport.class, metadata, inits);
    }

    public QPropertyReport(Class<? extends PropertyReport> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.property = inits.isInitialized("property") ? new QProperty(forProperty("property"), inits.get("property")) : null;
        this.reporter = inits.isInitialized("reporter") ? new com.homes.backend.domain.user.entity.QUser(forProperty("reporter")) : null;
    }

}

