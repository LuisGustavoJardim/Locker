package com.example.locker.ble.jsonclasses

import com.google.gson.annotations.SerializedName

data class ResponseCmdCtrl(

	@SerializedName("CmdCtrl")
	val cmdCtrl: CmdCtrl? = null
)

data class CmdCtrl(

	@SerializedName("id")
	val id: String? = null,

	@SerializedName("cmd")
	val cmd: Int? = null,

	@SerializedName("data")
	val data: String? = null,

	@SerializedName("error")
	val error: Int? = null,

	@SerializedName("done")
	val done: Boolean? = null
)
