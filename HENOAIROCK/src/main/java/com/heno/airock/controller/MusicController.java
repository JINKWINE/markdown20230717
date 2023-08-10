package com.heno.airock.controller;

import java.sql.SQLException;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.google.gson.Gson;
import com.heno.airock.cmn.PcwkLoger;
import com.heno.airock.dto.CodeVO;
import com.heno.airock.dto.MemberDTO;
import com.heno.airock.dto.MessageDTO;
import com.heno.airock.dto.MusicHeartDTO;
import com.heno.airock.dto.MusicVO;
import com.heno.airock.member.repository.MusicLikeCntRepository;
import com.heno.airock.service.CodeService;
import com.heno.airock.service.MusicLikeCntService;
import com.heno.airock.service.MusicService;

@Controller
@RequestMapping("/music")
public class MusicController implements PcwkLoger{

	@Autowired
	MusicService musicService;
	
	@Autowired
	CodeService codeService;
	
	private final MusicLikeCntService musicLikeCntService;

	public MusicController(MusicLikeCntService musicLikeCntService) {
		this.musicLikeCntService = musicLikeCntService;
	}
	
	@ResponseBody
	@RequestMapping(value= "/saveHeart", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public String save_heart(@RequestParam(value="musicId") String musicId, MusicHeartDTO mhDTO, HttpSession httpSession) {
		String jsonString = "";
		MemberDTO memberDTO = (MemberDTO) httpSession.getAttribute("user");
		MessageDTO message = new MessageDTO();
		
		mhDTO.setUserId(memberDTO.getUserId());
		mhDTO.setMusicId(musicId);
		LOG.debug("mhDTO:" + mhDTO);
		int saveHeartResult = musicLikeCntService.saveHeart(mhDTO);
		
		LOG.debug("saveHeartResult:" + saveHeartResult);
		if(saveHeartResult == 1) {
			message.setMsgId("1");
			message.setMsgContents("좋아요!");
			jsonString = new Gson().toJson(message);
			return jsonString;
		} else {
			message.setMsgId("2");
			message.setMsgContents("실패!");
			jsonString = new Gson().toJson(message);
			return jsonString;
		}
		
	}
	
	@ResponseBody
	@RequestMapping(value = "/deleteHeart", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
	public String delete_heart(@RequestParam(value="musicId") String musicId, MusicHeartDTO mhDTO, HttpSession httpSession) {
		String jsonString = "";
		MemberDTO memberDTO = (MemberDTO) httpSession.getAttribute("user");
		MessageDTO message = new MessageDTO();

		mhDTO.setUserId(memberDTO.getUserId());
		mhDTO.setMusicId(musicId);
		LOG.debug("mhDTO:" + mhDTO);
		
		int deleteHeartResult = musicLikeCntService.deleteHeart(mhDTO);

		if(deleteHeartResult == 1) {
			message.setMsgId("1");
			message.setMsgContents("좋아요 취소");
			jsonString = new Gson().toJson(message);
			return jsonString;
		} else {
			message.setMsgId("2");
			message.setMsgContents("실패!");
			jsonString = new Gson().toJson(message);
			return jsonString;
		}
	}


	@GetMapping("/music_detail")
	public String selectOne(@RequestParam(value="musicId") String musicId,
	@ModelAttribute MusicVO inVO, Model model, HttpSession httpSession) throws SQLException {
		String view = "/music/music_detail";
		MemberDTO memberDTO = (MemberDTO) httpSession.getAttribute("user");
		MusicHeartDTO to = new MusicHeartDTO();
		
		if(null !=inVO && null != musicId) {
			inVO.setMusicId(musicId);
			if (memberDTO== null) {
				return "redirect:/member/login";
			}
			to.setUserId(memberDTO.getUserId());
			to.setMusicId(musicId);
			httpSession.setAttribute("userId",memberDTO.getUserId());
			LOG.debug("inVO:" + inVO);
			LOG.debug("to:" + to);
			
			MusicVO musicDetail = musicService.selectOne(inVO);
			
			model.addAttribute("like", musicLikeCntService.findLike(to));
			model.addAttribute("musicDetail", musicDetail);
			model.addAttribute("inVO", inVO);
			
			return view;
			
		} else {
			
			return "/music/music";
		}
			
	}
	
	//음악 순위 게시판
	@GetMapping("/music_rank")
	public String music_rank(MusicVO inVO, Model model) throws SQLException {
		String viewPage = "/music/music_rank";
		// page번호
		if (null != inVO && inVO.getPageNo() == 0) {
			inVO.setPageNo(1);
		}

		// pageSize
		if (null != inVO && inVO.getPageSize() == 0) {
			inVO.setPageSize(10);
		}

		List<MusicVO> musicList = this.musicService.selectRank(inVO);
		
		model.addAttribute("musicList", musicList);
		model.addAttribute("inVO", inVO);
		
		return viewPage;
		
	}
	
	
	@GetMapping("")
	public String select(@RequestParam(value = "genre", 
		    required = false) String genre, MusicVO inVO, Model model) throws SQLException {
		String viewPage = "/music/music";
		// page번호
		if (null != inVO && inVO.getPageNo() == 0) {
			inVO.setPageNo(1);
		}

		// pageSize
		if (null != inVO && inVO.getPageSize() == 0) {
			inVO.setPageSize(50);
		}

		// searchWord
		if (null != inVO && null == inVO.getSearchWord()) {
			inVO.setSearchWord("");
		}

		// searchDiv
		if (null != inVO && null == inVO.getSearchDiv()) {
			inVO.setSearchDiv("");
		}
		
		// genre
		if (null != inVO && null != genre) {
			inVO.setGenre(genre);
		}
		LOG.debug("inVO:" + inVO);
		// 코드조회: 검색코드
		CodeVO codeVO = new CodeVO();
		codeVO.setCodeId("MUSIC_SEARCH");
		List<CodeVO> searchList = codeService.select(codeVO);
		model.addAttribute("searchList", searchList);
		
		
		List<MusicVO> musicList = this.musicService.select(inVO);
		model.addAttribute("musicList", musicList);
		
		//총글수
		int totalCnt = 0;
		if(null !=musicList && musicList.size() >0 ) {
			totalCnt = musicList.get(0).getTotalCnt();
		}
		
		model.addAttribute("totalCnt", totalCnt);
		model.addAttribute("inVO", inVO);
		return viewPage;
	}

}