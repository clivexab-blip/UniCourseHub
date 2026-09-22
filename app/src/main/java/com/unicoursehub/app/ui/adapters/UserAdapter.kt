package com.unicoursehub.app.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.unicoursehub.app.data.User
import com.unicoursehub.app.databinding.ItemUserBinding
import com.unicoursehub.app.util.CourseVisuals

class UserAdapter(
    private var items: List<User>,
    private val onMenuClick: ((User, android.view.View) -> Unit)? = null
) : RecyclerView.Adapter<UserAdapter.VH>() {

    inner class VH(val binding: ItemUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val user = items[position]
        with(holder.binding) {
            tvAvatar.text = CourseVisuals.initials(user.fullName)
            tvName.text = user.fullName
            tvEmail.text = user.email
            tvRole.text = user.role.replaceFirstChar { it.uppercase() }
            tvStatus.text = if (user.status == "active") {
                root.context.getString(com.unicoursehub.app.R.string.status_active_label)
            } else {
                root.context.getString(com.unicoursehub.app.R.string.status_suspended_label)
            }
            tvStatus.setBackgroundResource(CourseVisuals.statusChipBg(user.status))
            tvStatus.setTextColor(root.context.getColor(CourseVisuals.statusChipTextColor(user.status)))

            btnUserMenu.setOnClickListener { onMenuClick?.invoke(user, it) }
        }
    }

    override fun getItemCount() = items.size

    fun updateData(newItems: List<User>) {
        items = newItems
        notifyDataSetChanged()
    }
}
