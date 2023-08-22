package com.example.locker

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatButton
import com.example.locker.ble.LockerHard
import com.example.locker.ble.Mac10Controller
import com.example.locker.ble.RawBleController
import com.example.locker.ble.exceptions.BleNotConnectedException
import com.example.locker.ble.exceptions.MissingActivityException
import com.example.locker.ble.exceptions.MissingBlePermissionException
import com.example.locker.ble.interfaces.EventListenerBleInterface
import com.example.locker.ble.jsonclasses.ResponseCmdCtrl
import com.example.locker.ble.jsonclasses.ResponseCmdMem
import com.example.locker.databinding.ActivityMainBinding
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import java.util.*
import kotlin.math.log

//Definindo a classe principal da atividade, que estende AppCompatActivity e implementa
// EventListenerBleInterface
class MainActivity : AppCompatActivity(), EventListenerBleInterface {


    // Declaração de variáveis e propriedades
    var controller: Mac10Controller? = null
    var isServiceBound: Boolean = false
    //var uuid: String = "52d8bc40-5e09-11ea-bc55-0242ac100090"
    //var uuid: String   = "52d8bc40-5e09-11ea-bc55-0242ac100136"
//    var uuid: String   = "52d8bc41-5e09-11ea-bc55-0242ac100136"
//    var wantedMac: String   = "EC:3A:23:26:EA:79"
    var relayNumber = 1
    private var isOpeningRelay = false

    private var isConnected = false

    private lateinit var binding: ActivityMainBinding

    // Método de criação da atividade
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflar o layout usando o Binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Configurar ouvintes para botões
        setListeners()
    }

    private fun setListeners() {
        // Configurar ouvintes para os botões de abrir os relês
        binding.btnOpen2.setOnClickListener { openRelay(1)
            Log.d("clicou no 2", "button 2")
        }
        binding.btnOpen4.setOnClickListener { openRelay(3)
            Log.d("clicou no 4", "button 4")}
        binding.btnOpen6.setOnClickListener { openRelay(5)
            Log.d("clicou no 6", "button 6")}

        binding.btnOpen1.setOnClickListener { openRelay(2)
            Log.d("clicou no 1", "button 1")}
        binding.btnOpen3.setOnClickListener { openRelay(4)
            Log.d("clicou no 3", "button 3")}
        binding.btnOpen5.setOnClickListener { openRelay(6)
            Log.d("clicou no 5", "button 5")}


        // Configurar ouvintes para os botões de alternar entre lados (par e ímpar)
        binding.btLadoPar.setOnClickListener {
            requestPermission("52d8bc40-5e09-11ea-bc55-0242ac100136", "EC:3A:23:26:EA:79")
            // Configurar a visibilidade dos botões de relê de acordo com o lado selecionado
            binding.btnOpen1.visibility = View.GONE
            binding.btnOpen3.visibility = View.GONE
            binding.btnOpen5.visibility = View.GONE

            binding.btnOpen2.visibility = View.VISIBLE
            binding.btnOpen4.visibility = View.VISIBLE
            binding.btnOpen6.visibility = View.VISIBLE
        }
        binding.btLadoImpar.setOnClickListener {
            requestPermission("52d8bc40-5e09-11ea-bc55-0242ac100128", "DD:E3:D2:84:2B:F2")
            // Configurar a visibilidade dos botões de relê de acordo com o lado selecionado
            binding.btnOpen2.visibility = View.GONE
            binding.btnOpen4.visibility = View.GONE
            binding.btnOpen6.visibility = View.GONE

            binding.btnOpen1.visibility = View.VISIBLE
            binding.btnOpen3.visibility = View.VISIBLE
            binding.btnOpen5.visibility = View.VISIBLE
        }
    }

    // Iniciar o serviço Bluetooth para a conexão com o dispositivo
    private fun startBluetoothService(locker: LockerHard) {
        Log.i("LOG", "startBluetoothService")
        try {
            // Criar uma instância do controlador Mac10Controller
            controller = Mac10Controller(this)
            // Adicionar um ouvinte para eventos BLE
            controller?.addListener(this)
            // Configurar o controlador BLE com o armário especificado
            RawBleController.locker = locker
            // Tentar estabelecer a conexão BLE com um timeout de 5 segundos
            controller?.connectBle("mac10", "", "", 5000)
        } catch (e: Exception) {
            Log.e("ERROR ", Objects.requireNonNull(e.message.toString()));
            startBluetoothService(locker);
        }

        /*Log.d("LOG", "startBluetoothService")
        try {
            controller?.connectBle("mac10", "", "", 5000)
            // Em caso de erro, registrar a mensagem de erro e tentar novamente
        } catch (e: Exception) {
            Log.e("ERROR ", Objects.requireNonNull(e.message.toString()));
            startBluetoothService();
        }*/
    }

    // Conectar-se a um dispositivo BLE com um UUID e endereço MAC específicos
    fun connectDevice(uuidMac: String, addressMac: String) {
        Log.d("LOG", "connectDevice")

        // Verificar se o Bluetooth está habilitado no dispositivo
        if (isBluetoothEnabled()) {

//            val locker = LockerHard(
//                "52d8bc40-5e09-11ea-bc55-0242ac100136", "EC:3A:23:26:EA:79",
//                "52d8bc40-5e09-11ea-bc55-0242ac100136", "EC:3A:23:26:EA:79",
//                "", ""
//            )
// Criar uma instância do armário LockerHard com os UUIDs e endereços MAC
            val locker = LockerHard(
                uuidMac, addressMac,
                "52d8bc40-5e09-11ea-bc55-0242ac100136", "EC:3A:23:26:EA:79",
                "", ""
            )

            try {
                // Criar uma instância do controlador Mac10Controller
                controller = Mac10Controller(this)
                // Adicionar um ouvinte para eventos BLE
                controller?.addListener(this)
                // Configurar o controlador BLE com o armário especificado
                RawBleController.locker = locker
                // Tentar estabelecer a conexão BLE com um timeout de 5 segundos
                controller?.connectBle("mac10", "", "", 5000)
            } catch (e: Exception) {
                // Em caso de erro, registrar a mensagem de erro e iniciar o serviço Bluetooth
                Log.e("ERROR ", Objects.requireNonNull(e.message.toString()));
                startBluetoothService(locker)
            }

        } else {
            /* Builder(this)
                 .setMessage("Para usar essa função ative o Bluetooth de seu celular.")
                 .setCancelable(false)
                 .setPositiveButton("Ok", DialogInterface.OnClickListener { dialog, which ->
                     val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                     startActivityForResult(enableBtIntent, 1)
                 })
                 .show()*/
        }
    }


    // Solicitar permissões relacionadas ao Bluetooth e localização
    fun requestPermission(uuidMac : String, addressMac: String) {
        Dexter.withActivity(this).withPermissions(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ).withListener(object : MultiplePermissionsListener {

            override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                connectDevice(uuidMac, addressMac)
            }

            override fun onPermissionRationaleShouldBeShown(
                permissions: MutableList<PermissionRequest>?,
                token: PermissionToken?
            ) {

                // Exibir um diálogo explicando que todas as permissões são obrigatórias
                AlertDialog.Builder(this@MainActivity)
                    .setMessage("Todas as permissões são obrigatórias")
                    .setCancelable(false)
                    .setPositiveButton("Ok", object : DialogInterface.OnClickListener {

                        override fun onClick(dialog: DialogInterface?, which: Int) {
                            requestPermission(uuidMac, addressMac)
                        }

                    })
                    .show()

            }
        }).check()
    }

    // Verificar se o Bluetooth está habilitado no dispositivo
    fun isBluetoothEnabled(): Boolean {
        val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        return mBluetoothAdapter.isEnabled
    }

    fun openRelay(relay: Int) {
        isOpeningRelay = true

        // Agendar a ação de abrir o relê em uma thread principal com um atraso de 500ms
        runOnUiThread {
            Handler().postDelayed(
                {
                    try {

                        isOpeningRelay = false
                        // Chamar o comando básico para abrir o relê especificado
                        controller?.basicCommandRelay(relay)
                    } catch (e: BleNotConnectedException) {
                        // Em caso de erro de conexão BLE, registrar a mensagem de erro
                        Log.e("ERROR ", e.message.toString())
                    }
                    isOpeningRelay = false
                }, 500
            )
        }

    }

    override fun onConnectedAndReady() {
        Log.d("LOG", "Ble onConnectedAndReady")

        runOnUiThread {
            Handler().postDelayed(
                {
                    try {
                        controller?.asyncExecCommand("mac@2020", 5)
                        isConnected = true
                    } catch (e: BleNotConnectedException) {
                        assert(e.message != null)
                        Log.e("ERROR ", e.message.toString())
                    }
                }, 500
            )
        }
    }

    override fun onConnected() {
        Log.d("LOG", "Ble Connected")
    }

    override fun onDisconnected() {
        Log.d("LOG", "Ble Disconnected")

        //if (isServiceBound) startBluetoothService()
    }

    override fun onConnectFail(msg: String?) {
        Log.d("LOG", "Ble Fail")
    }

    override fun onDiscoveryStarted() {
        Log.d("LOG", "Ble onDiscoveryStarted")
    }

    override fun onDiscoveryFinished() {
        Log.d("LOG", "Ble onDiscoveryFinished")
    }

    override fun onDeviceDiscovered() {
        Log.d("LOG", "Ble onDeviceDiscovered")
    }

    override fun onDiscoveryDeviceFail() {
        Log.d("LOG", "Ble onDiscoveryDeviceFail")
    }

    override fun onDeviceConnectionFail() {
        Log.d("LOG", "Ble onDeviceConnectionFail")
    }

    override fun onGetServicesFail(status: String?) {
        Log.d("LOG", "Ble onGetServicesFail")
    }

    override fun onReceivedData(data: String) {
        Log.d("LOG", "Ble onReceivedData")

        var responseCmdCtrl = ResponseCmdCtrl()
        var responseCmdMem = ResponseCmdMem()

        val gson = Gson()

        if (data.contains("CmdMem")) {
            responseCmdMem = gson.fromJson(data, ResponseCmdMem::class.java)
        } else if (data.contains("CmdCtrl")) {
            responseCmdCtrl = gson.fromJson(data, ResponseCmdCtrl::class.java)
        } else {
            return
        }

        val response = responseCmdCtrl.cmdCtrl

        if (response != null) {

            if (response.cmd != null && response.error != null) {

                if (response.cmd == 1) {
                    if (response.error != 0) {
                        //openRelay()

                    } else {
                        controller?.removeListener(this)
                        controller?.disconnectBle()
                        controller = null
                    }
                } else {
                    if (response.error == 0) {
                        //openRelay()
                        //controller?.writeMemoryCommand(103, "8000")
                    } else {
                        onConnectedAndReady()
                    }
                }

            }

        }

    }

    override fun onLog(log: String?) {
        Log.d("LOG", "Ble onLog $log")
    }
}