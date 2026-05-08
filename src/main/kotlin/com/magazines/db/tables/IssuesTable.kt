package com.magazines.db.tables

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime

object IssuesTable : UUIDTable("issues") {
    val magazineId = reference("magazine_id", MagazinesTable, onDelete = ReferenceOption.CASCADE)
    val issueNumber = varchar("issue_number", 50)
    val publicationDate = date("publication_date").nullable()
    val pdfPath = varchar("pdf_path", 500)
    val pagesCount = integer("pages_count").nullable()
    val createdAt = datetime("created_at").defaultExpression(CurrentDateTime)
}
