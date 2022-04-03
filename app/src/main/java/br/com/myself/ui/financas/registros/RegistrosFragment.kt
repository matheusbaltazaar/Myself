package br.com.myself.ui.financas.registros

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.myself.R
import br.com.myself.model.entity.Registro
import br.com.myself.model.repository.RegistroRepository
import br.com.myself.observer.Events
import br.com.myself.observer.Trigger
import br.com.myself.ui.adapter.RegistroAdapter
import br.com.myself.util.AdapterClickListener
import br.com.myself.util.Async
import br.com.myself.util.Utils
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_registros.*
import org.jetbrains.anko.support.v4.intentFor
import org.jetbrains.anko.support.v4.toast
import java.util.*

class RegistrosFragment(private val repository: RegistroRepository) : Fragment() {
    
    private val disposable = CompositeDisposable()
    private val monthPageController: MonthPageController = MonthPageController()
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_registros, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        registrarObservers()
        
        // INSTANCIAR ADAPTER E CLICKLISTENERS
        configureRegistroAdapter()
        
        // RECUPERAR OS DADOS DO REPOSITORIO
        atualizarDadosDoMes(monthPageController)
        
        // MONTH NAVIGATOR
        button_page_mes_anterior.setOnClickListener {
            atualizarDadosDoMes(monthPageController.mesAnterior())
            resetarLayoutIrPara()
        }
        button_page_proximo_mes.setOnClickListener {
            atualizarDadosDoMes(monthPageController.proximoMes())
            resetarLayoutIrPara()
        }
    
        // LABEL MÊS/ANO ACIONA DROPDOWNS
        textview_mes_ano.setOnClickListener {
            textview_mes_ano.visibility = View.GONE
            component_dropdown_mes_ano.visibility = View.VISIBLE
        }
    
        // DROPDOWN MÊS
        dropdown_pesquisa_mes.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Utils.MESES_STRING))
        dropdown_pesquisa_mes.setOnItemClickListener { _, _, position, _ ->
            atualizarDadosDoMes(monthPageController.goTo(month = position))
        }
    
        // DROPDOWN ANO
        dropdown_pesquisa_ano.setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, Utils.ANOS))
        dropdown_pesquisa_ano.setOnItemClickListener { _, _, _, _ ->
            atualizarDadosDoMes(monthPageController.goTo(year = dropdown_pesquisa_ano.text.toString().toInt()))
        }
    
        // AÇÃO BOTÃO PESQUISAR
        button_registros_pesquisar.setOnClickListener {
            requireActivity().startActivity(intentFor<PesquisarRegistrosActivity>())
        }
        
        // AÇÃO BOTÃO ADICIONAR (+)
        fab_registros_add_registro.setOnClickListener {
            abrirBottomSheetCriarRegistro(null)
        }
    }
    
    private fun configureRegistroAdapter() {
        recyclerview_registros.layoutManager = LinearLayoutManager(requireContext())
        val adapter = RegistroAdapter()
        val listener = AdapterClickListener<Registro>(
            onClick = {
                val dialog = DetalhesRegistroDialog(requireContext(), it)
                dialog.show()
            },
            onLongClick = {
                var mensagem = "Descrição: ${it.descricao}"
                mensagem += "\nValor: ${Utils.formatCurrency(it.valor)}"
    
                AlertDialog.Builder(requireContext()).setTitle("Excluir registro?").setMessage(mensagem)
                    .setPositiveButton("Excluir") { _, _ ->
                        Async.doInBackground({
                            repository.excluirRegistro(it)
                        }, {
                            toast("Removido!")
                            Trigger.launch(Events.UpdateRegistros)
                        })
                    }.setNegativeButton("Cancelar", null).show()
            }
        )
        adapter.setClickListener(listener)
        recyclerview_registros.adapter = adapter
    }
    
    @SuppressLint("SetTextI18n")
    private fun atualizarDadosDoMes(controller: MonthPageController) {
        textview_mes_ano.setText(Utils.MESES_STRING[controller.getMonth()] + "/" + controller.getYear())
        tv_registros_total_registros_mes.setText("")
        tv_registros_quantidade_registros_mes.setText("")
        dropdown_pesquisa_mes.setText(Utils.MESES_STRING[controller.getMonth()], false)
        dropdown_pesquisa_ano.setText("${controller.getYear()}", false)
        
        Async.doInBackground({
            repository.pesquisarRegistros(controller.getMonth(), controller.getYear())
        },
        { registros -> // CALLBACK APÓS A CONSULTA
            val total = registros.map(Registro::valor).sum()
    
            tv_registros_total_registros_mes.setText(Utils.formatCurrency(total))
            tv_registros_quantidade_registros_mes.setText("${registros.size}")
    
            (recyclerview_registros.adapter as RegistroAdapter).submitList(registros) {
                textview_nenhum_registro_encontrado.visibility =
                    if (registros.isEmpty()) View.VISIBLE else View.GONE
            }
            
        })
    }
    
    private fun resetarLayoutIrPara() {
        textview_mes_ano.visibility = View.VISIBLE
        component_dropdown_mes_ano.visibility = View.GONE
    }
    
    private fun registrarObservers() {
        disposable.clear()
        disposable.add(
            Trigger.watcher().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread()).subscribe { t ->
                when (t) {
                    is Events.UpdateRegistros -> {
                        atualizarDadosDoMes(monthPageController)
                    }
                    is Events.EditarRegistro -> abrirBottomSheetCriarRegistro(t.registro)
                }
            })
    }
    
    private fun abrirBottomSheetCriarRegistro(registro: Registro?) {
        val bottomSheet = CriarRegistroBottomSheet(registro, repository)
        bottomSheet.show(childFragmentManager, null)
    }
    
    override fun onResume() {
        super.onResume()
        resetarLayoutIrPara()
    }
    
    override fun onDestroyView() {
        disposable.clear()
        super.onDestroyView()
    }
    
    private class MonthPageController(val calendar: Calendar = Utils.getCalendar()) {
        fun proximoMes(): MonthPageController {
            calendar.roll(Calendar.MONTH, true)
            return this
        }
        
        fun mesAnterior(): MonthPageController {
            calendar.roll(Calendar.MONTH, false)
            return this
        }
    
        fun getMonth(): Int {
            return calendar[Calendar.MONTH]
        }
    
        fun getYear(): Int {
            return calendar[Calendar.YEAR]
        }
    
        fun goTo(month: Int = getMonth(), year: Int = getYear()): MonthPageController {
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            return this
        }
    }
    
}
