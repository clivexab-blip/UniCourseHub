package com.unicoursehub.app.ui.admin

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.unicoursehub.app.R
import com.unicoursehub.app.data.DatabaseHelper
import com.unicoursehub.app.databinding.FragmentManageUsersBinding
import com.unicoursehub.app.ui.adapters.UserAdapter

class ManageUsersFragment : Fragment() {

    private var _binding: FragmentManageUsersBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: DatabaseHelper
    private lateinit var adapter: UserAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageUsersBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = DatabaseHelper.getInstance(requireContext())

        adapter = UserAdapter(
            items = db.getAllUsers(),
            onMenuClick = { user, view ->
                showUserMenu(user, view)
            }
        )
        binding.rvUsers.layoutManager = LinearLayoutManager(requireContext())
        binding.rvUsers.adapter = adapter

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { refresh(s.toString()) }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun refresh(query: String) {
        adapter.updateData(if (query.isBlank()) db.getAllUsers() else db.searchUsers(query))
    }

    private fun showUserMenu(user: com.unicoursehub.app.data.User, view: View) {
        val popup = android.widget.PopupMenu(requireContext(), view)
        popup.menu.add("Edit Profile")
        popup.menu.add(if (user.status == "active") "Suspend User" else "Reactivate User")
        popup.menu.add("Delete User")

        popup.setOnMenuItemClickListener { item ->
            when (item.title) {
                "Edit Profile" -> showEditDialog(user)
                "Suspend User", "Reactivate User" -> {
                    db.setUserStatus(user.id, if (user.status == "active") "suspended" else "active")
                    refresh(binding.etSearch.text.toString())
                }
                "Delete User" -> {
                    val confirmView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_action, null)
                    val confirmDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
                        .setView(confirmView)
                        .create()
                    
                    confirmView.findViewById<android.widget.TextView>(R.id.tvTitle).text = getString(R.string.btn_delete)
                    confirmView.findViewById<android.widget.TextView>(R.id.tvMessage).text = getString(R.string.msg_confirm_delete_account)
                    confirmView.findViewById<android.widget.ImageView>(R.id.ivIcon).apply {
                        setImageResource(R.drawable.ic_close_circle)
                        setColorFilter(requireContext().getColor(R.color.danger))
                    }
                    
                    confirmView.findViewById<View>(R.id.btnCancel).setOnClickListener { confirmDialog.dismiss() }
                    confirmView.findViewById<android.widget.Button>(R.id.btnConfirm).apply {
                        text = getString(R.string.btn_delete)
                        setOnClickListener {
                            db.deleteUser(user.id)
                            refresh(binding.etSearch.text.toString())
                            confirmDialog.dismiss()
                        }
                    }
                    
                    confirmDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    confirmDialog.show()
                }
            }
            true
        }
        popup.show()
    }

    private fun showEditDialog(user: com.unicoursehub.app.data.User) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_user, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
            .setView(dialogView)
            .create()

        val etName = dialogView.findViewById<EditText>(R.id.etName)
        val etUni = dialogView.findViewById<EditText>(R.id.etUniversity)
        val btnSave = dialogView.findViewById<View>(R.id.btnSave)
        val btnDelete = dialogView.findViewById<View>(R.id.btnDelete)
        val btnClose = dialogView.findViewById<View>(R.id.btnClose)

        etName.setText(user.fullName)
        etUni.setText(user.university)

        btnSave.setOnClickListener {
            db.updateUser(user.copy(fullName = etName.text.toString(), university = etUni.text.toString()))
            refresh(binding.etSearch.text.toString())
            dialog.dismiss()
        }

        btnDelete.setOnClickListener {
            val confirmView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_action, null)
            val confirmDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext(), R.style.Theme_UniCourseHub_NoBar)
                .setView(confirmView)
                .create()
            
            confirmView.findViewById<android.widget.TextView>(R.id.tvTitle).text = getString(R.string.btn_delete)
            confirmView.findViewById<android.widget.TextView>(R.id.tvMessage).text = getString(R.string.msg_confirm_delete_account)
            confirmView.findViewById<android.widget.ImageView>(R.id.ivIcon).apply {
                setImageResource(R.drawable.ic_close_circle)
                setColorFilter(requireContext().getColor(R.color.danger))
            }
            
            confirmView.findViewById<View>(R.id.btnCancel).setOnClickListener { confirmDialog.dismiss() }
            confirmView.findViewById<android.widget.Button>(R.id.btnConfirm).apply {
                text = getString(R.string.btn_delete)
                setOnClickListener {
                    db.deleteUser(user.id)
                    refresh(binding.etSearch.text.toString())
                    confirmDialog.dismiss()
                    dialog.dismiss()
                }
            }
            
            confirmDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            confirmDialog.show()
        }

        btnClose.setOnClickListener { dialog.dismiss() }
        
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
