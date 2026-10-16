package com.nuvio.tv.domain.model

class Meta(val apiType: String, val id: String)
class NextToWatch(val nextVideoId: String?, val nextSeason: Int?, val nextEpisode: Int?, val watchProgress: Any? = null)
class Video(val id: String, val season: Int?, val episode: Int?)
