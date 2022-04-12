package br.com.myself.ui.financas

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.myself.R
import br.com.myself.domain.repository.DespesaRepository
import br.com.myself.domain.repository.EntradaRepository
import br.com.myself.domain.repository.RegistroRepository
import br.com.myself.ui.financas.despesas.DespesasFragment
import br.com.myself.ui.financas.entradas.EntradasFragment
import br.com.myself.ui.financas.registros.RegistrosFragment
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.android.synthetic.main.activity_financas.*

class FinancasActivity : AppCompatActivity() {
    
    private val registroRepository: RegistroRepository by lazy { RegistroRepository(applicationContext) }
    private val despesaRepository: DespesaRepository by lazy { DespesaRepository(applicationContext) }
    private val entradaRepository: EntradaRepository by lazy { EntradaRepository(applicationContext) }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_financas)
        
        initView()
    }
    
    private fun initView() {
        bottom_navigation_view_financas.setOnItemSelectedListener { menuItem ->
            
            when (menuItem.itemId) {
                R.id.bottom_navigation_registros -> {
                    supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                        .replace(R.id.container_finanacas, RegistrosFragment(registroRepository)).commit()
                    
                    textview_financas_titulo.text = "Registros"
                }
                R.id.bottom_navigation_entradas -> {
                    supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                        .replace(R.id.container_finanacas, EntradasFragment(entradaRepository)).commit()
                    
                    textview_financas_titulo.text = "Entradas"
                }
                R.id.bottom_navigation_despesas -> {
                    supportFragmentManager.beginTransaction().setReorderingAllowed(true)
                        .replace(R.id.container_finanacas, DespesasFragment(despesaRepository, registroRepository)).commit()
                    
                    textview_financas_titulo.text = "Despesas"
                }
            }
            
            true
        }
        
        bottom_navigation_view_financas.selectedItemId = R.id.bottom_navigation_registros
    }
    
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
    }
    
}
