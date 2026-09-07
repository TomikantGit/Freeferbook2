package com.livrohub.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Banco de dados Room principal do LivroHub.
 *
 * Contém as seguintes entidades:
 * - [BookEntity]: livros/manuscritos
 * - [ChapterEntity]: capítulos de um livro
 * - [ChapterVersionEntity]: versões salvas de um capítulo (histórico imutável)
 * - [ImageEntity]: imagens de referência de um livro
 * - [LocationEntity]: locais/cenários do livro
 *
 * **Versão 6**: adicionou LocationEntity.
 *
 * @see AppContainer para criação da instância singleton.
 */
@Database(
    entities = [
        BookEntity::class,
        ChapterEntity::class,
        ChapterVersionEntity::class,
        CharacterEntity::class,
        ImageEntity::class,
        LocationEntity::class
    ],
    version = 6,
    exportSchema = true
)
abstract class LivroHubDatabase : RoomDatabase() {
    /** DAO para operações de livros. */
    abstract fun bookDao(): BookDao

    /** DAO para operações de capítulos. */
    abstract fun chapterDao(): ChapterDao

    /** DAO para operações de versões de capítulos. */
    abstract fun chapterVersionDao(): ChapterVersionDao

    /** DAO para operações de personagens. */
    abstract fun characterDao(): CharacterDao

    /** DAO para operações de imagens. */
    abstract fun imageDao(): ImageDao

    /** DAO para operações de locais. */
    abstract fun locationDao(): LocationDao
}
