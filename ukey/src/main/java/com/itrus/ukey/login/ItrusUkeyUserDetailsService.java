package com.itrus.ukey.login;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.itrus.ukey.db.Admin;
import com.itrus.ukey.db.AdminExample;
import com.itrus.ukey.db.AdminRole;
import com.itrus.ukey.db.AdminRoleExample;
import com.itrus.ukey.db.RoleAndResources;
import com.itrus.ukey.db.RoleAndResourcesExample;
import com.itrus.ukey.db.SysResources;
import com.itrus.ukey.util.CacheCustomer;
import com.itrus.ukey.util.LicenseData;
import com.itrus.ukey.util.UkeyUser;

public class ItrusUkeyUserDetailsService implements UserDetailsService {
    @Autowired
	SqlSession sqlSession;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private CacheCustomer cacheCustomer;
	
	public UserDetails loadUserByUsername(String username)
			throws UsernameNotFoundException {
		//资源编号集合
		Collection<Integer> resNums = new HashSet<Integer>();
		// 查询用户信息
		AdminExample adminex = new AdminExample();
		adminex.or().andAccountEqualTo(username.toLowerCase());	
		Admin admin = sqlSession.selectOne("com.itrus.ukey.db.AdminMapper.selectByExample", adminex);
		boolean isNonLocked = true; 
		// 用户授权信息
		Collection authorities = new ArrayList();
		// 用户不存在，异常处理
		if(admin==null){
			Integer count = sqlSession.selectOne("com.itrus.ukey.db.AdminMapper.countByExample",null);
			if(count>0)
				throw new UsernameNotFoundException(username);
			
			admin = new Admin();
			admin.setPassword("itrusyes");
			admin.setStatus("valid");
			admin.setCreateTime(new Date());
			LicenseData license = LicenseData.getDefault();
			resNums = license.getResNums();
			for(String title: license.getRoleTitle())
				authorities.add(new SimpleGrantedAuthority(title));
		}else{//项目管理员
			AdminRoleExample roleex = new AdminRoleExample();
			roleex.or().andIdEqualTo(admin.getAdminRole());
			AdminRole adminRole = sqlSession.selectOne("com.itrus.ukey.db.AdminRoleMapper.selectByExample", roleex);
			RoleAndResourcesExample rarEx = new RoleAndResourcesExample();
			rarEx.or().andAdminRoleEqualTo(adminRole.getId());
			List<RoleAndResources> roleAndRes = sqlSession.selectList("com.itrus.ukey.db.RoleAndResourcesMapper.selectByExample",rarEx);
			for(RoleAndResources rar:roleAndRes){
				SysResources res = cacheCustomer.getResById(rar.getSysResources());
				resNums.add(res.getResNum());
				authorities.add(new SimpleGrantedAuthority(res.getResRoleName()));
			}
			/* //获取license中允许的菜单项，运营版不需要，删除
			LicenseData license = cacheCustomer.getLicense();
			if(license.getEndTime().before(new Date()))
				license = LicenseData.getDefault();
			Collection<Integer> resNumsLicense = license.getResNums();
			for(RoleAndResources rar:roleAndRes){
				SysResources res = cacheCustomer.getResById(rar.getSysResources());
				if(resNumsLicense.contains(res.getResNum())){
					resNums.add(res.getResNum());
					authorities.add(new SimpleGrantedAuthority(res.getResRoleName()));
				}
			}*/
		}
		
		String pass = admin.getPassword();
		
		if(pass!=null&&pass.length()!=40)
//			pass = PassUtil.doDigestSHA1(pass,username);
			pass = passwordEncoder.encodePassword(pass,username);
		isNonLocked = "valid".equalsIgnoreCase(admin.getStatus())?true:false;
		return new UkeyUser(admin.getId(),username,pass,isNonLocked,admin.getProject(),admin.getCreateTime(),resNums,authorities);
	}

}
