package org.smart.erp.eip.adapter;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.smart.erp.eip.dto.RecipientSelectorDTO;
import org.smart.erp.eip.enums.RecipientSelectorType;
import org.smart.erp.eip.port.RecipientDirectory;
import org.smart.erp.eip.vo.RecipientOptionVO;
import org.smart.erp.system.entity.Dept;
import org.smart.erp.system.entity.Permission;
import org.smart.erp.system.entity.RoleInfo;
import org.smart.erp.system.entity.RolePermission;
import org.smart.erp.system.entity.User;
import org.smart.erp.system.entity.UserRole;
import org.smart.erp.system.Enum.DeptStatus;
import org.smart.erp.system.Enum.PermissionType;
import org.smart.erp.system.Enum.RoleEnum;
import org.smart.erp.system.Enum.Status;
import org.smart.erp.system.Enum.UserStatus;
import org.smart.erp.system.mapper.DeptMapper;
import org.smart.erp.system.mapper.PermissionMapper;
import org.smart.erp.system.mapper.RoleInfoMapper;
import org.smart.erp.system.mapper.RolePermissionMapper;
import org.smart.erp.system.mapper.UserMapper;
import org.smart.erp.system.mapper.UserRoleMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 系统目录的 MyBatis 适配器，是 EIP 包中唯一感知 sys_* 系统表的实现类。
 */
@Component
public class RecipientDirectoryAdapter implements RecipientDirectory {

	private final UserMapper userMapper;

	private final RoleInfoMapper roleMapper;

	private final PermissionMapper permissionMapper;

	private final DeptMapper deptMapper;

	private final UserRoleMapper userRoleMapper;

	private final RolePermissionMapper rolePermissionMapper;

	public RecipientDirectoryAdapter(UserMapper userMapper, RoleInfoMapper roleMapper,
			PermissionMapper permissionMapper, DeptMapper deptMapper, UserRoleMapper userRoleMapper,
			RolePermissionMapper rolePermissionMapper) {
		this.userMapper = userMapper;
		this.roleMapper = roleMapper;
		this.permissionMapper = permissionMapper;
		this.deptMapper = deptMapper;
		this.userRoleMapper = userRoleMapper;
		this.rolePermissionMapper = rolePermissionMapper;
	}

	@Override
	public Set<Long> resolve(RecipientSelectorDTO selector) {
		if (selector == null)
			return Collections.emptySet();
		Set<Long> candidates = new LinkedHashSet<>();
		if (selector.isAllActiveUsers()) {
			candidates
				.addAll(userMapper.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, UserStatus.NORMAL))
					.stream()
					.map(User::getId)
					.toList());
		}
		if (selector.getUserIds() != null && !selector.getUserIds().isEmpty()) {
			candidates.addAll(selector.getUserIds());
		}
		if (selector.getRoleCodes() != null && !selector.getRoleCodes().isEmpty()) {
			candidates.addAll(usersByRoles(roleIds(selector.getRoleCodes())));
		}
		if (selector.getPermissionCodes() != null && !selector.getPermissionCodes().isEmpty()) {
			Set<Long> permissionIds = permissionMapper
				.selectList(new LambdaQueryWrapper<Permission>().in(Permission::getCode, selector.getPermissionCodes())
					.eq(Permission::getStatus, Status.ENABLE))
				.stream()
				.map(Permission::getId)
				.collect(Collectors.toSet());
			if (!permissionIds.isEmpty()) {
				Set<Long> roleIds = rolePermissionMapper
					.selectList(
							new LambdaQueryWrapper<RolePermission>().in(RolePermission::getPermissionId, permissionIds))
					.stream()
					.map(RolePermission::getRoleId)
					.collect(Collectors.toSet());
				candidates.addAll(usersByRoles(roleIds));
			}
		}
		if (selector.getDepartmentIds() != null && !selector.getDepartmentIds().isEmpty()) {
			Set<Long> departmentIds = selector.isIncludeChildDepartments()
					? includeDescendants(selector.getDepartmentIds()) : new HashSet<>(selector.getDepartmentIds());
			candidates.addAll(userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getDeptId, departmentIds))
				.stream()
				.map(User::getId)
				.toList());
		}
		// 管理员扩展为可选开启；特别地，仅指定 USER 时不会进入该分支。
		if (selector.isIncludeAdministrators()) {
			Set<Long> adminRoles = roleMapper
				.selectList(new LambdaQueryWrapper<RoleInfo>()
					.and(w -> w.eq(RoleInfo::getCode, "admin").or().eq(RoleInfo::getName, "管理员"))
					.eq(RoleInfo::getStatus, RoleEnum.ENABLE))
				.stream()
				.map(RoleInfo::getId)
				.collect(Collectors.toSet());
			candidates.addAll(usersByRoles(adminRoles));
		}
		if (candidates.isEmpty())
			return Collections.emptySet();
		return userMapper
			.selectList(
					new LambdaQueryWrapper<User>().in(User::getId, candidates).eq(User::getStatus, UserStatus.NORMAL))
			.stream()
			.map(User::getId)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private Set<Long> roleIds(Set<String> codes) {
		return roleMapper
			.selectList(new LambdaQueryWrapper<RoleInfo>().in(RoleInfo::getCode, codes)
				.eq(RoleInfo::getStatus, RoleEnum.ENABLE))
			.stream()
			.map(RoleInfo::getId)
			.collect(Collectors.toSet());
	}

	private Set<Long> usersByRoles(Set<Long> roleIds) {
		if (roleIds.isEmpty())
			return Collections.emptySet();
		return userRoleMapper.selectList(new LambdaQueryWrapper<UserRole>().in(UserRole::getRoleId, roleIds))
			.stream()
			.map(UserRole::getUserId)
			.collect(Collectors.toSet());
	}

	private Set<Long> includeDescendants(Set<Long> roots) {
		List<Dept> enabled = deptMapper
			.selectList(new LambdaQueryWrapper<Dept>().eq(Dept::getStatus, DeptStatus.ENABLE));
		Set<Long> result = new HashSet<>(roots);
		ArrayDeque<Long> queue = new ArrayDeque<>(roots);
		while (!queue.isEmpty()) {
			Long parent = queue.removeFirst();
			for (Dept dept : enabled) {
				if (parent.equals(dept.getParentId()) && result.add(dept.getId()))
					queue.addLast(dept.getId());
			}
		}
		return result;
	}

	@Override
	public List<RecipientOptionVO> options(RecipientSelectorType type, String keyword) {
		if (type == null)
			return Collections.emptyList();
		String query = keyword == null ? "" : keyword.trim();
		return switch (type) {
			case USER -> userMapper
				.selectList(new LambdaQueryWrapper<User>().eq(User::getStatus, UserStatus.NORMAL)
					.and(!query.isBlank(), w -> w.like(User::getUsername, query).or().like(User::getRealName, query)))
				.stream()
				.map(u -> new RecipientOptionVO(type, String.valueOf(u.getId()),
						display(u.getRealName(), u.getUsername())))
				.toList();
			case ROLE -> roleMapper
				.selectList(new LambdaQueryWrapper<RoleInfo>().eq(RoleInfo::getStatus, RoleEnum.ENABLE)
					.and(!query.isBlank(), w -> w.like(RoleInfo::getCode, query).or().like(RoleInfo::getName, query)))
				.stream()
				.map(r -> new RecipientOptionVO(type, r.getCode(), r.getName()))
				.toList();
			case PERMISSION -> permissionMapper
				.selectList(new LambdaQueryWrapper<Permission>().eq(Permission::getStatus, Status.ENABLE)
					.and(!query.isBlank(),
							w -> w.like(Permission::getCode, query).or().like(Permission::getName, query)))
				.stream()
				.map(p -> new RecipientOptionVO(type, p.getCode(), p.getName()))
				.toList();
			case DEPARTMENT -> deptMapper
				.selectList(new LambdaQueryWrapper<Dept>().eq(Dept::getStatus, DeptStatus.ENABLE)
					.and(!query.isBlank(), w -> w.like(Dept::getCode, query).or().like(Dept::getName, query)))
				.stream()
				.map(d -> new RecipientOptionVO(type, String.valueOf(d.getId()), d.getName()))
				.toList();
			case ALL_ACTIVE_USERS -> List.of(new RecipientOptionVO(type, "ALL_ACTIVE_USERS", "所有启用用户"));
			case ADMINISTRATORS -> List.of(new RecipientOptionVO(type, "ADMINISTRATORS", "管理员"));
		};
	}

	private String display(String realName, String username) {
		return realName == null || realName.isBlank() ? username : realName + " (" + username + ")";
	}

}