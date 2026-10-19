package org.semiproject.pillbadeumserver.drug.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 공공데이터포털에서 동기화한 의약품 낱알 정보.
 * itemSeq는 식품의약품안전처 품목기준코드이며 공공데이터의 ITEM_SEQ와 대응한다.
 */
@Entity
@Table(name = "drug")
public class Drug {

    @Id
    @Column(name = "item_seq", length = 100, nullable = false, updatable = false)
    private String itemSeq;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "maker", length = 100, nullable = false)
    private String maker;

    /** ETC 또는 OTC. 공공데이터의 ETC_OTC_NAME은 동기화 과정에서 이 코드로 변환한다. */
    @Column(name = "etc_otc", length = 10, nullable = false)
    private String etcOtc;

    @Column(name = "class_name", length = 100, nullable = false)
    private String className;

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "print_front", length = 50)
    private String printFront;

    @Column(name = "print_back", length = 50)
    private String printBack;

    @Column(name = "shape", length = 50, nullable = false)
    private String shape;

    @Column(name = "color", length = 50, nullable = false)
    private String color;

    @Column(name = "line_front", length = 10)
    private String lineFront;

    @Column(name = "line_back", length = 10)
    private String lineBack;

    @Column(name = "form", length = 30, nullable = false)
    private String form;

    @Column(name = "material", length = 100, nullable = false)
    private String material;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    protected Drug() {
    }

    public Drug(String itemSeq, String name, String maker, String etcOtc, String className,
                String imageUrl, String printFront, String printBack, String shape, String color,
                String lineFront, String lineBack, String form, String material) {
        this.itemSeq = itemSeq;
        this.name = name;
        this.maker = maker;
        this.etcOtc = etcOtc;
        this.className = className;
        this.imageUrl = imageUrl;
        this.printFront = printFront;
        this.printBack = printBack;
        this.shape = shape;
        this.color = color;
        this.lineFront = lineFront;
        this.lineBack = lineBack;
        this.form = form;
        this.material = material;
    }

    /** 같은 ITEM_SEQ의 공공데이터를 재동기화할 때 호출한다. */
    public void update(String name, String maker, String etcOtc, String className,
                       String imageUrl, String printFront, String printBack, String shape, String color,
                       String lineFront, String lineBack, String form, String material) {
        this.name = name;
        this.maker = maker;
        this.etcOtc = etcOtc;
        this.className = className;
        this.imageUrl = imageUrl;
        this.printFront = printFront;
        this.printBack = printBack;
        this.shape = shape;
        this.color = color;
        this.lineFront = lineFront;
        this.lineBack = lineBack;
        this.form = form;
        this.material = material;
        this.syncedAt = LocalDateTime.now();
    }

    @PrePersist
    private void initializeSyncedAt() {
        if (syncedAt == null) {
            syncedAt = LocalDateTime.now();
        }
    }

    public String getItemSeq() {
        return itemSeq;
    }

    public String getName() {
        return name;
    }

    public String getMaker() {
        return maker;
    }

    public String getEtcOtc() {
        return etcOtc;
    }

    public String getClassName() {
        return className;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getPrintFront() {
        return printFront;
    }

    public String getPrintBack() {
        return printBack;
    }

    public String getShape() {
        return shape;
    }

    public String getColor() {
        return color;
    }

    public String getLineFront() {
        return lineFront;
    }

    public String getLineBack() {
        return lineBack;
    }

    public String getForm() {
        return form;
    }

    public String getMaterial() {
        return material;
    }

    public LocalDateTime getSyncedAt() {
        return syncedAt;
    }
}
