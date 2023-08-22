package com.example.locker.ble.jsonclasses

import com.google.gson.annotations.SerializedName

data class ResponseCmdMem(

	@SerializedName("CmdMem")
	val cmdMem: CmdMem? = null
)

data class CmdMem(

	@SerializedName("address")
	val address: Int? = null,

	@SerializedName("data")
	val data: String? = null,

	@SerializedName("id")
	val id: String? = null,

	@SerializedName("error")
	val error: Int? = null,

	@SerializedName("done")
	val done: Boolean? = null
)
