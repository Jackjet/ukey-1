package com.itrus.ukey.db;

import java.util.Date;

public class MessageTemplate {
    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column message_template.id
     *
     * @mbggenerated
     */
    private Long id;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column message_template.message_content
     *
     * @mbggenerated
     */
    private String messageContent;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column message_template.message_type
     *
     * @mbggenerated
     */
    private String messageType;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column message_template.project
     *
     * @mbggenerated
     */
    private Long project;

    /**
     * This field was generated by MyBatis Generator.
     * This field corresponds to the database column message_template.create_time
     *
     * @mbggenerated
     */
    private Date createTime;

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column message_template.id
     *
     * @return the value of message_template.id
     *
     * @mbggenerated
     */
    public Long getId() {
        return id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column message_template.id
     *
     * @param id the value for message_template.id
     *
     * @mbggenerated
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column message_template.message_content
     *
     * @return the value of message_template.message_content
     *
     * @mbggenerated
     */
    public String getMessageContent() {
        return messageContent;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column message_template.message_content
     *
     * @param messageContent the value for message_template.message_content
     *
     * @mbggenerated
     */
    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column message_template.message_type
     *
     * @return the value of message_template.message_type
     *
     * @mbggenerated
     */
    public String getMessageType() {
        return messageType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column message_template.message_type
     *
     * @param messageType the value for message_template.message_type
     *
     * @mbggenerated
     */
    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column message_template.project
     *
     * @return the value of message_template.project
     *
     * @mbggenerated
     */
    public Long getProject() {
        return project;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column message_template.project
     *
     * @param project the value for message_template.project
     *
     * @mbggenerated
     */
    public void setProject(Long project) {
        this.project = project;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method returns the value of the database column message_template.create_time
     *
     * @return the value of message_template.create_time
     *
     * @mbggenerated
     */
    public Date getCreateTime() {
        return createTime;
    }

    /**
     * This method was generated by MyBatis Generator.
     * This method sets the value of the database column message_template.create_time
     *
     * @param createTime the value for message_template.create_time
     *
     * @mbggenerated
     */
    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}