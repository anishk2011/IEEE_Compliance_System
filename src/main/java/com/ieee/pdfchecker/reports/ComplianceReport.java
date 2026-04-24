package com.ieee.pdfchecker.reports;

import com.ieee.pdfchecker.rules.RuleCode;

import java.util.ArrayList;
import java.util.List;

public class ComplianceReport {
    private Long paperId;
    private Long versionId;
    private String fileName;
    private List<String> errors;
    private List<String> infoMessages;
    private List<ComplianceItem> items;
    private Integer score;

    // Rule-specific boolean flags
    private boolean abstractPresent;
    private boolean fontCompliant;
    private boolean columnFormatCompliant;
    private boolean keywordsPresent;
    private boolean authorDetailsCompliant;
    private boolean checkIntroPresenceValid;
    private boolean pageSizeCompliant;

    public ComplianceReport(String fileName) {
        this.fileName = fileName;
        this.errors = new ArrayList<>();
        this.infoMessages = new ArrayList<>();
        this.items = new ArrayList<>();
    }

    public void addError(String error) {
        errors.add(error);
        RuleCode ruleCode = RuleCode.resolve(null, error);
        items.add(new ComplianceItem("fail", ruleCode.getRuleName(), ruleCode.getCode(), error));
    }

    public void addInfo(String info) {
        infoMessages.add(info);
        RuleCode ruleCode = RuleCode.resolve(null, info);
        items.add(new ComplianceItem("pass", ruleCode.getRuleName(), ruleCode.getCode(), info));
    }

    public boolean isCompliant() {
        return errors.isEmpty();
    }

    public String getFileName() {
        return fileName;
    }

    public Long getPaperId() {
        return paperId;
    }

    public void setPaperId(Long paperId) {
        this.paperId = paperId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public List<String> getErrors() {
        return errors;
    }

    public List<String> getInfoMessages() {
        return infoMessages;
    }

    public List<ComplianceItem> getItems() {
        return items;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public boolean isAbstractPresent() {
        return abstractPresent;
    }

    public void setAbstractPresent(boolean abstractPresent) {
        this.abstractPresent = abstractPresent;
    }

    public boolean isFontCompliant() {
        return fontCompliant;
    }

    public void setFontCompliant(boolean fontCompliant) {
        this.fontCompliant = fontCompliant;
    }

    public boolean isColumnFormatCompliant() {
        return columnFormatCompliant;
    }

    public void setColumnFormatCompliant(boolean columnFormatCompliant) {
        this.columnFormatCompliant = columnFormatCompliant;
    }

    public boolean isKeywordsPresent() {
        return keywordsPresent;
    }

    public void setKeywordsPresent(boolean keywordsPresent) {
        this.keywordsPresent = keywordsPresent;
    }

    public boolean isAuthorDetailsCompliant() {
        return authorDetailsCompliant;
    }

    public void setAuthorDetailsCompliant(boolean authorDetailsCompliant) {
        this.authorDetailsCompliant = authorDetailsCompliant;
    }

    public boolean ischeckIntroPresenceValid() {
        return checkIntroPresenceValid;
    }

    public void setcheckIntroPresenceValid(boolean checkIntroPresenceValid) {
        this.checkIntroPresenceValid = checkIntroPresenceValid;
    }

    public boolean isPageSizeCompliant() {
        return pageSizeCompliant;
    }

    public void setPageSizeCompliant(boolean pageSizeCompliant) {
        this.pageSizeCompliant = pageSizeCompliant;
    }
}
