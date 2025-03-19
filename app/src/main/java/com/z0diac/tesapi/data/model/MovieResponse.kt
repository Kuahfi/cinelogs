package com.z0diac.tesapi.data.model

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class MovieResponse(
    @SerializedName("results") val results: List<Movie1>
)

@Parcelize
data class Movie1(
    val id: Int,
    val title: String,
    @SerializedName("poster_path") val posterPath: String,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("release_date") val releaseDate: String,
    @SerializedName("vote_average") val rating: Float?,
    val genres: List<Genre>? = emptyList(),
    @SerializedName("overview") val overview: String,
    @SerializedName("runtime") val runtime: Int? = 0,
    val credits: Credits? = null
) : Parcelable

@Parcelize
data class Credits(
    val crew: List<CrewMember> = emptyList(),
    val cast: List<Cast> = emptyList()
) : Parcelable

@Parcelize
data class CrewMember(
    val job: String,
    val name: String
) : Parcelable

@Parcelize
data class Genre(
    val id: Int,
    val name: String
) : Parcelable

data class VideoResponse(
    val results: List<Video>
)

data class Video(
    val key: String,
    val site: String,
    val type: String
)


@Parcelize
data class Cast(
    val id: Int,
    val name: String,
    @SerializedName("character") val character: String,
    @SerializedName("profile_path") val profilePath: String?
) : Parcelable